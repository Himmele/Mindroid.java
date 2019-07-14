/*
 * Copyright (C) 2018 E.S.R.Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mindroid.runtime.system;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import mindroid.os.Binder;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.Parcel;
import mindroid.os.RemoteException;
import mindroid.util.Log;
import mindroid.util.concurrent.Promise;

public class Runtime {
    private static final String LOG_TAG = "Runtime";
    private static final String MINDROID_SCHEME = "mindroid";
    private static final String MINDROID_SCHEME_WITH_SEPARATOR = "mindroid://";
    private static Runtime sRuntime;
    private final int mNodeId;
    private final Map<String, Plugin> mPlugins = new ConcurrentHashMap<>();
    private final Map<Long, WeakReference<Binder>> mBinderIds = new ConcurrentHashMap<>();
    private final Map<String, WeakReference<Binder>> mBinderUris = new ConcurrentHashMap<>();
    private final Map<String, Binder> mServices = new HashMap<>();
    private final Map<String, WeakReference<Binder.Proxy>> mProxies = new HashMap<>();
    private final AtomicInteger mBinderIdGenerator = new AtomicInteger(1);
    private final AtomicInteger mProxyIdGenerator = new AtomicInteger(1);
    private final Set<Long> mIds = ConcurrentHashMap.newKeySet();
    private ServiceDiscovery.Configuration mConfiguration;

    private Runtime(int nodeId, File configurationFile) {
        if (nodeId == 0) {
            throw new IllegalArgumentException("Mindroid runtime system node id must not be 0");
        }
        mNodeId = nodeId;
        Log.println('I', LOG_TAG, "Mindroid runtime system node id: " + mNodeId);
        if (configurationFile != null) {
            try {
                mConfiguration = ServiceDiscovery.read(configurationFile);
            } catch (Exception e) {
                Log.println('E', LOG_TAG, "Failed to read Mindroid runtime system configuration", e);
            }
        }
        if (mConfiguration != null && mConfiguration.nodes.containsKey(mNodeId)) {
            for (ServiceDiscovery.Configuration.Plugin plugin : mConfiguration.nodes.get(mNodeId).plugins.values()) {
                try {
                    Class<Plugin> clazz = (Class<Plugin>) Class.forName(plugin.clazz);
                    mPlugins.put(plugin.scheme, clazz.newInstance());
                } catch (Exception e) {
                    Log.println('E', LOG_TAG, "Cannot find class \'" + plugin.clazz + "\': " + e.getMessage(), e);
                } catch (LinkageError e) {
                    Log.println('E', LOG_TAG, "Linkage error: " + e.getMessage(), e);
                }
            }

            Set<Long> ids = new HashSet<>();
            for (ServiceDiscovery.Configuration.Service service : mConfiguration.services.values()) {
                if (service.node.id == nodeId) {
                    long id = ((long) nodeId << 32) | (service.id & 0xFFFFFFFFL);
                    ids.add(id);
                }
            }
            mIds.addAll(ids);
        }
    }

    public static Runtime getRuntime() {
        return sRuntime;
    }

    public static void start(int nodeId, File configuration) {
        boolean start = false;
        synchronized (Runtime.class) {
            if (sRuntime == null) {
                sRuntime = new Runtime(nodeId, configuration);
                start = true;
            }
        }
        if (start) {
            for (Plugin plugin : sRuntime.mPlugins.values()) {
                plugin.setUp(sRuntime);
                plugin.start();
            }
        }
    }

    public static void shutdown() {
        boolean shutdown = false;
        Runtime runtime = sRuntime;
        synchronized (Runtime.class) {
            if (sRuntime != null) {
                sRuntime = null;
                shutdown = true;
            }
        }
        if (shutdown) {
            for (Plugin plugin : runtime.mPlugins.values()) {
                plugin.stop();
                plugin.tearDown();
            }
        }
    }

    public int getNodeId() {
        return mNodeId;
    }

    public ServiceDiscovery.Configuration getConfiguration() {
        return mConfiguration;
    }

    public final long attachBinder(Binder binder) {
        if (binder == null) {
            throw new NullPointerException();
        }
        long id;
        do {
            id = ((long) mNodeId << 32) | (mBinderIdGenerator.getAndIncrement() & 0xFFFFFFFFL);
        } while (mIds.contains(id));
        if (!mBinderIds.containsKey(id)) {
            mBinderIds.put(id, new WeakReference<Binder>(binder));
            mIds.add(id);
        } else {
            Log.wtf(LOG_TAG, "Invalid binder id: " + id);
        }
        return id;
    }

    public final void attachBinder(URI uri, Binder binder) {
        if (uri == null || binder == null) {
            throw new NullPointerException();
        }
        if (binder.getUri() == null) {
            throw new IllegalArgumentException("Binder URI must not be null");
        }
        if (!mBinderUris.containsKey(uri.toString())) {
            mBinderUris.put(uri.toString(), new WeakReference<Binder>(binder));

            Plugin plugin = mPlugins.get(binder.getUri().getScheme());
            if (plugin != null) {
                plugin.attachBinder(binder);
            }
        } else {
            Log.wtf(LOG_TAG, "Multiple Binder registration for URI: " + uri.toString());
        }
    }

    public final void detachBinder(long id, URI uri) {
        mIds.remove(id);
        mBinderIds.remove(id);
        if (uri != null) {
            mBinderUris.remove(uri.toString());
            Plugin plugin = mPlugins.get(uri.getScheme());
            if (plugin != null) {
                plugin.detachBinder(id);
            }
        }
    }

    public final Binder getBinder(long id) {
        int nodeId = (int) ((id >> 32) & 0xFFFFFFFFL);
        if (nodeId == 0 || mNodeId == nodeId) {
            WeakReference<Binder> binder = mBinderIds.get(((long) mNodeId << 32) | id);
            if (binder != null) {
                return binder.get();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public final IBinder getBinder(URI uri) {
        if (uri != null) {
            int nodeId;
            String authority = uri.getAuthority();
            String[] parts = authority.split("\\.");
            if (parts.length == 2) {
                try {
                    nodeId = Integer.valueOf(parts[0]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid URI: " + uri.toString());
                }
            } else {
                throw new IllegalArgumentException("Invalid URI: " + uri.toString());
            }
            if (mNodeId == nodeId) {
                final String key = uri.getScheme() + "://" + uri.getAuthority();
                WeakReference<Binder> b = mBinderUris.get(key);
                IBinder binder;
                if (MINDROID_SCHEME.equals(uri.getScheme())) {
                    if (b != null && (binder = b.get()) != null) {
                        return binder;
                    } else {
                        mBinderUris.remove(key);
                        return null;
                    }
                } else {
                    if (b != null && (binder = b.get()) != null) {
                        return binder;
                    } else {
                        mBinderUris.remove(key);
                        b = mBinderUris.get(MINDROID_SCHEME_WITH_SEPARATOR + uri.getAuthority());
                        if (b != null && (binder = b.get()) != null) {
                            Plugin plugin = mPlugins.get(uri.getScheme());
                            if (plugin != null) {
                                Binder stub = plugin.getStub((Binder) binder);
                                if (stub != null) {
                                    mBinderUris.put(key, new WeakReference<Binder>(stub));
                                }
                                return stub;
                            } else {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                }
            } else {
                return new Binder.Proxy(uri);
            }
        } else {
            return null;
        }
    }

    public final synchronized void addService(URI uri, IBinder service) {
        if (uri == null || service == null) {
            throw new NullPointerException();
        }
        if (service.getUri() == null) {
            throw new IllegalArgumentException("Service URI must not be null");
        }
        if (uri.getScheme() == null || !uri.getScheme().equals(service.getUri().getScheme())) {
            throw new IllegalArgumentException("Binder scheme mismatch: " + uri + " != " + service.getUri());
        }
        if (service instanceof Binder) {
            if (!mServices.containsKey(uri.toString())) {
                if (mConfiguration != null) {
                    ServiceDiscovery.Configuration.Node node = mConfiguration.nodes.get(mNodeId);
                    if (node != null) {
                        ServiceDiscovery.Configuration.Service s = node.services.get(uri.getAuthority());
                        if (s != null) {
                            long oldId = ((long) mNodeId << 32) | (service.getId() & 0xFFFFFFFFL);
                            mIds.remove(oldId);
                            mBinderIds.remove(oldId);
                            mBinderUris.remove(service.getUri().toString());
                            long newId = ((long) mNodeId << 32) | (s.id & 0xFFFFFFFFL);
                            ((Binder) service).setId(newId);
                            mIds.add(newId);
                            mBinderIds.put(newId, new WeakReference<Binder>((Binder) service));
                            mBinderUris.put(service.getUri().toString(), new WeakReference<Binder>((Binder) service));
                        }
                    }
                }
                mServices.put(uri.toString(), (Binder) service);
            }
        } else {
            throw new IllegalArgumentException("Service must be of superclass mindroid.os.Binder");
        }
    }

    public final synchronized void removeService(IBinder service) {
        if (service != null) {
            Iterator<Map.Entry<String, Binder>> itr = mServices.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, Binder> entry = itr.next();
                if (entry.getValue().getId() == service.getId()) {
                    itr.remove();
                }
            }
        }
    }

    public final synchronized IBinder getService(URI uri) {
        final URI serviceUri;
        if (uri.getScheme() != null) {
            serviceUri = uri;
        } else {
            try {
                serviceUri = new URI(MINDROID_SCHEME,
                        uri.getUserInfo(),
                        uri.getHost(),
                        uri.getPort(),
                        uri.getPath(),
                        uri.getQuery(),
                        uri.getFragment());
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid service URI");
            }
        }

        // Services
        IBinder binder = mServices.get(serviceUri.toString());
        if (binder != null) {
            return binder;
        } else {
            binder = mServices.get(MINDROID_SCHEME_WITH_SEPARATOR + serviceUri.getAuthority());
            if (binder != null) {
                Plugin plugin = mPlugins.get(serviceUri.getScheme());
                if (plugin != null) {
                    Binder stub = plugin.getStub((Binder) binder);
                    if (stub != null) {
                        mServices.put(serviceUri.toString(), stub);
                    }
                    return stub;
                } else {
                    return null;
                }
            }
        }

        // Proxies
        return getProxy(serviceUri);
    }

    public final long attachProxy(Binder.Proxy proxy) {
        long proxyId = mProxyIdGenerator.getAndIncrement();
        Plugin plugin = mPlugins.get(proxy.getUri().getScheme());
        if (plugin != null) {
            plugin.attachProxy(proxyId, proxy);
        }
        return proxyId;
    }

    public final void detachProxy(long id, URI uri, long proxyId) {
        Plugin plugin = mPlugins.get(uri.getScheme());
        if (plugin != null) {
            plugin.detachProxy(proxyId, id);
        }
    }

    public final IInterface getProxy(IBinder binder) {
        Plugin plugin = mPlugins.get(binder.getUri().getScheme());
        if (plugin != null) {
            return plugin.getProxy(binder);
        } else {
            return null;
        }
    }

    /**
     * Establishes a connection to node.
     *
     * Erlang Documentation: http://erlang.org/doc/reference_manual/distributed.html#node-connections, http://erlang.org/doc/man/net_kernel.html#connect_node-1.
     * Elixir Documentation: https://hexdocs.pm/elixir/Node.html.
     *
     * @param node The node.
     * @param extras Extra parameters.
     */
    public final Promise<Void> connect(URI node, Bundle extras) {
        Plugin plugin = mPlugins.get(node.getScheme());
        if (plugin != null) {
            return plugin.connect(node, extras);
        } else {
            return new Promise<>(new RemoteException("Node connection failure"));
        }
    }

    /**
     * Forces the disconnection of a node.
     *
     * Erlang Documentation: http://erlang.org/doc/reference_manual/distributed.html#node-connections, http://erlang.org/doc/man/net_kernel.html#connect_node-1.
     * Elixir Documentation: https://hexdocs.pm/elixir/Node.html.
     *
     * @param node The node.
     * @param extras Extra parameters.
     */
    public final Promise<Void> disconnect(URI node, Bundle extras) {
        Plugin plugin = mPlugins.get(node.getScheme());
        if (plugin != null) {
            return plugin.disconnect(node, extras);
        } else {
            return new Promise<>(new RemoteException("Node disconnection failure"));
        }
    }

    public final Promise<Parcel> transact(IBinder binder, int what, Parcel data, int flags) throws RemoteException {
        Plugin plugin = mPlugins.get(binder.getUri().getScheme());
        if (plugin != null) {
            Promise<Parcel> promise = plugin.transact(binder, what, data, flags);
            if (flags != Binder.FLAG_ONEWAY && promise == null) {
                throw new RemoteException("Binder transaction failure");
            }
            return promise;
        } else {
            throw new RemoteException("Binder transaction failure");
        }
    }

    public final void link(IBinder binder, IBinder.Supervisor supervisor, Bundle extras) throws RemoteException {
        Plugin plugin = mPlugins.get(binder.getUri().getScheme());
        if (plugin != null) {
            plugin.link(binder, supervisor, extras);
        } else {
            throw new RemoteException("Binder linking failure");
        }
    }

    public final boolean unlink(IBinder binder, IBinder.Supervisor supervisor, Bundle extras) {
        Plugin plugin = mPlugins.get(binder.getUri().getScheme());
        if (plugin != null) {
            return plugin.unlink(binder, supervisor, extras);
        } else {
            return true;
        }
    }

    private synchronized Binder.Proxy getProxy(URI uri) {
        final String key = uri.toString();
        WeakReference<Binder.Proxy> wp = mProxies.get(key);
        Binder.Proxy p;
        if (wp != null && (p = wp.get()) != null) {
            return p;
        } else {
            mProxies.remove(key);
        }

        if (mConfiguration != null) {
            ServiceDiscovery.Configuration.Service service = mConfiguration.services.get(uri.getAuthority());
            if (service == null) {
                return null;
            }

            try {
                URI interfaceDescriptor = new URI(service.announcements.get(uri.getScheme()));
                URI proxyUri = new URI(uri.getScheme(), service.node.id + "." + service.id, "/if=" + interfaceDescriptor.getPath().substring(1), interfaceDescriptor.getQuery(), null);
                Binder.Proxy proxy = new Binder.Proxy(proxyUri);
                mProxies.put(key, new WeakReference<Binder.Proxy>(proxy));
                return proxy;
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public final synchronized void removeProxy(IBinder proxy) {
        mProxies.values().removeIf(p -> proxy.equals(p.get()));
    }
}
