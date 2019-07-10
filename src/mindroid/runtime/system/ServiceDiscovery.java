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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ServiceDiscovery {
    private static final String LOG_TAG = "ServiceDiscovery";
    private static final String ROOT_TAG = "runtime";
    private static final String NODES_TAG = "nodes";
    private static final String NODE_TAG = "node";
    private static final String NODE_ID_ATTR = "id";
    private static final String PLUGIN_TAG = "plugin";
    private static final String PLUGIN_SCHEME_ATTR = "scheme";
    private static final String PLUGIN_CLASS_ATTR = "class";
    private static final String SERVER_TAG = "server";
    private static final String SERVER_URI_ATTR = "uri";
    private static final String SERVICE_DISCOVERY_TAG = "serviceDiscovery";
    private static final String SERVICE_TAG = "service";
    private static final String SERVICE_ID_ATTR = "id";
    private static final String SERVICE_NAME_ATTR = "name";
    private static final String ANNOUNCEMENT_TAG = "announcement";
    private static final String ANNOUNCEMENT_INTERFACE_DESCRIPTOR_ATTR = "interfaceDescriptor";

    public static class Configuration {
        public static class Node {
            public int id;
            public Map<String, Plugin> plugins = new HashMap<>();
            public Map<String, Service> services = new HashMap<>();
        }

        public static class Plugin {
            public Node node;
            public String scheme;
            public String clazz;
            public Server server;
        }

        public static class Server {
            public String uri;
        }

        public static class Service {
            public Node node;
            public int id;
            public String name;
            public Map<String, String> announcements = new HashMap<>();
        }

        public Map<Integer, Configuration.Node> nodes = new HashMap<>();
        public Map<String, Service> services = new HashMap<>();
    }

    public static Configuration read(File file) throws Exception {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput((InputStream) inputStream, "UTF-8");
            parser.require(XmlPullParser.START_DOCUMENT, null, null);
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, ROOT_TAG);

            Configuration configuration = new Configuration();
            for (int eventType = parser.nextTag(); !parser.getName().equals(ROOT_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
                if (parser.getName().equals(NODES_TAG)) {
                    parseNodes(parser, configuration);
                } else if (parser.getName().equals(SERVICE_DISCOVERY_TAG)) {
                    parseServiceDiscovery(parser, configuration);
                } else {
                    String tag = parser.getName();
                    skipSubTree(parser);
                    parser.require(XmlPullParser.END_TAG, null, tag);
                }
            }

            parser.require(XmlPullParser.END_TAG, null, ROOT_TAG);
            parser.next();
            parser.require(XmlPullParser.END_DOCUMENT, null, null);

            return configuration;
        } catch (Exception e) {
            throw e;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static void parseNodes(XmlPullParser parser, Configuration configuration) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, NODES_TAG);

        for (int eventType = parser.nextTag(); !parser.getName().equals(NODES_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(NODE_TAG)) {
                Configuration.Node node = parseNode(parser);
                if (node != null) {
                    configuration.nodes.put(node.id, node);
                }
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, NODES_TAG);
    }

    private static Configuration.Node parseNode(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, NODE_TAG);

        Configuration.Node node = new Configuration.Node();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(NODE_ID_ATTR)) {
                try {
                    node.id = Integer.parseInt(attributeValue);
                } catch (NumberFormatException e) {
                }
            }
        }
        if (node.id == 0) {
            throw new XmlPullParserException("Invalid node: " + node.id);
        }

        for (int eventType = parser.nextTag(); !parser.getName().equals(NODE_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(PLUGIN_TAG)) {
                Configuration.Plugin plugin = parsePlugin(parser);
                if (plugin != null) {
                    plugin.node = node;
                    node.plugins.put(plugin.scheme, plugin);
                }
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, NODE_TAG);
        return node;
    }

    private static Configuration.Plugin parsePlugin(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, PLUGIN_TAG);

        Configuration.Plugin plugin = new Configuration.Plugin();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(PLUGIN_SCHEME_ATTR)) {
                plugin.scheme = attributeValue;
            } else if (attributeName.equals(PLUGIN_CLASS_ATTR)) {
                plugin.clazz = attributeValue;
            }
        }
        if (plugin.scheme == null || plugin.scheme.isEmpty()
                || plugin.clazz == null || plugin.clazz.isEmpty()) {
            throw new XmlPullParserException("Invalid plugin: " + plugin.clazz);
        }

        for (int eventType = parser.nextTag(); !parser.getName().equals(PLUGIN_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(SERVER_TAG)) {
                plugin.server = parseServer(parser);
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, PLUGIN_TAG);
        return plugin;
    }

    private static Configuration.Server parseServer(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, SERVER_TAG);

        Configuration.Server server = new Configuration.Server();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(SERVER_URI_ATTR)) {
                server.uri = attributeValue;
            }
        }
        if (server.uri == null || server.uri.isEmpty()) {
            throw new XmlPullParserException("Invalid server: " + server.uri);
        }

        for (int eventType = parser.nextTag(); !parser.getName().equals(SERVER_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            String tag = parser.getName();
            skipSubTree(parser);
            parser.require(XmlPullParser.END_TAG, null, tag);
        }

        parser.require(XmlPullParser.END_TAG, null, SERVER_TAG);
        return server;
    }

    private static void parseServiceDiscovery(XmlPullParser parser, Configuration configuration) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, SERVICE_DISCOVERY_TAG);

        for (int eventType = parser.nextTag(); !parser.getName().equals(SERVICE_DISCOVERY_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(NODE_TAG)) {
                parseServiceDiscoveryNode(parser, configuration);
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, SERVICE_DISCOVERY_TAG);
    }

    private static void parseServiceDiscoveryNode(XmlPullParser parser, Configuration configuration) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, NODE_TAG);

        int nodeId = 0;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(NODE_ID_ATTR)) {
                try {
                    nodeId = Integer.valueOf(attributeValue);
                } catch (NumberFormatException e) {
                }
            }
        }
        if (!configuration.nodes.containsKey(nodeId)) {
            throw new XmlPullParserException("Invalid node: " + nodeId);
        }

        Configuration.Node node = configuration.nodes.get(nodeId);
        for (int eventType = parser.nextTag(); !parser.getName().equals(NODE_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(SERVICE_TAG)) {
                Configuration.Service service = parseService(parser);
                if (service != null) {
                    service.node = node;
                    node.services.put(service.name, service);
                    configuration.services.put(service.name, service);
                }
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, NODE_TAG);
    }

    private static Configuration.Service parseService(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, SERVICE_TAG);

        Configuration.Service service = new Configuration.Service();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(SERVICE_ID_ATTR)) {
                try {
                    service.id = Integer.valueOf(attributeValue);
                } catch (NumberFormatException e) {
                }
            } else if (attributeName.equals(SERVICE_NAME_ATTR)) {
                service.name = attributeValue;
            }
        }
        if (service.id == 0 || service.name == null || service.name.isEmpty()) {
            throw new XmlPullParserException("Invalid service: " + service.id);
        }

        for (int eventType = parser.nextTag(); !parser.getName().equals(SERVICE_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(ANNOUNCEMENT_TAG)) {
                URI interfaceDescriptor = parseAnnouncement(parser);
                if (interfaceDescriptor != null) {
                    service.announcements.put(interfaceDescriptor.getScheme(), interfaceDescriptor.toString());
                }
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, SERVICE_TAG);
        return service;
    }

    private static URI parseAnnouncement(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, ANNOUNCEMENT_TAG);

        String interfaceDescriptor = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(ANNOUNCEMENT_INTERFACE_DESCRIPTOR_ATTR)) {
                interfaceDescriptor = attributeValue;
            }
        }
        if (interfaceDescriptor == null || interfaceDescriptor.isEmpty()) {
            throw new XmlPullParserException("Invalid announcement: " + interfaceDescriptor);
        }

        for (int eventType = parser.nextTag(); !parser.getName().equals(ANNOUNCEMENT_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            String tag = parser.getName();
            skipSubTree(parser);
            parser.require(XmlPullParser.END_TAG, null, tag);
        }

        parser.require(XmlPullParser.END_TAG, null, ANNOUNCEMENT_TAG);
        try {
            return URI.create(interfaceDescriptor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void skipSubTree(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, null);
        int level = 1;
        while (level > 0) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.END_TAG) {
                --level;
            } else if (eventType == XmlPullParser.START_TAG) {
                ++level;
            }
        }
    }
}
