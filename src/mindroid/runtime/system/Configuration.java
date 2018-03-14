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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class Configuration {
    private static final String LOG_TAG = "Configuration";
    private static final String ROOT_TAG = "runtime";
    private static final String PLUGINS_TAG = "plugins";
    private static final String PLUGIN_TAG = "plugin";
    private static final String PLUGIN_NAME_ATTR = "name";
    private static final String PLUGIN_SCHEME_ATTR = "scheme";
    private static final String PLUGIN_CLASS_ATTR = "class";
    private static final String PLUGIN_ENABLED_ATTR = "enabled";
    private static final String SERVICE_DISCOVERY_TAG = "serviceDiscovery";
    private static final String NODE_TAG = "node";
    private static final String NODE_ID_ATTR = "id";
    private static final String NODE_URI_ATTR = "uri";
    private static final String SERVICE_TAG = "service";
    private static final String SERVICE_ID_ATTR = "id";
    private static final String SERVICE_NAME_ATTR = "name";
    private static final String SERVICE_INTERFACE_ATTR = "interface";

    public static class Plugin {
        public String name;
        public String scheme;
        public String clazz;
        public boolean enabled;
        public Map<Integer, Node> nodes;
        public Map<String, Service> services;
    }

    public static class Node {
        public int id;
        public String uri;
        public List<Service> services;
    }

    public static class Service {
        public Node node;
        public int id;
        public String name;
        public String interfaceDescriptor;
    }

    public Map<String, Plugin> plugins = new HashMap<>();

    public static Configuration read(File configurationFile) throws Exception {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configurationFile);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput((InputStream) inputStream, "UTF-8");
            parser.require(XmlPullParser.START_DOCUMENT, null, null);
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, ROOT_TAG);

            Configuration configuration = new Configuration();
            for (int eventType = parser.nextTag(); !parser.getName().equals(ROOT_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
                if (parser.getName().equals(PLUGINS_TAG)) {
                    parsePlugins(parser, configuration);
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

    private static void parsePlugins(XmlPullParser parser, Configuration configuration) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, PLUGINS_TAG);

        for (int eventType = parser.nextTag(); !parser.getName().equals(PLUGINS_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(PLUGIN_TAG)) {
                Plugin plugin = parsePlugin(parser);
                if (plugin != null) {
                    configuration.plugins.put(plugin.scheme, plugin);
                }
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, PLUGINS_TAG);
    }

    private static Plugin parsePlugin(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, PLUGIN_TAG);

        Plugin plugin = new Plugin();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(PLUGIN_NAME_ATTR)) {
                plugin.name = attributeValue;
            } else if (attributeName.equals(PLUGIN_SCHEME_ATTR)) {
                plugin.scheme = attributeValue;
            } else if (attributeName.equals(PLUGIN_CLASS_ATTR)) {
                plugin.clazz = attributeValue;
            } else if (attributeName.equals(PLUGIN_ENABLED_ATTR)) {
                plugin.enabled = Boolean.valueOf(attributeValue);
            }
        }
        if (plugin.name == null || plugin.name.isEmpty()
                || plugin.scheme == null || plugin.scheme.isEmpty()
                || plugin.clazz == null || plugin.clazz.isEmpty()) {
            throw new XmlPullParserException("Invalid plugin: " + plugin.name);
        }

        for (int eventType = parser.nextTag(); !parser.getName().equals(PLUGIN_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(SERVICE_DISCOVERY_TAG)) {
                parseServiceDiscovery(parser, plugin);
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, PLUGIN_TAG);
        return plugin;
    }

    private static void parseServiceDiscovery(XmlPullParser parser, Plugin plugin) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, SERVICE_DISCOVERY_TAG);

        plugin.services = new HashMap<>();
        Map<Integer, Node> nodes = new HashMap<>();
        for (int eventType = parser.nextTag(); !parser.getName().equals(SERVICE_DISCOVERY_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(NODE_TAG)) {
                Node node = parseNode(parser);
                if (node != null) {
                    nodes.put(node.id, node);
                    for (Service service : node.services) {
                        plugin.services.put(service.name, service);
                    }
                }
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }
        plugin.nodes = nodes;

        parser.require(XmlPullParser.END_TAG, null, SERVICE_DISCOVERY_TAG);
    }

    private static Node parseNode(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, NODE_TAG);

        Node node = new Node();
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(NODE_ID_ATTR)) {
                try {
                    node.id = Integer.valueOf(attributeValue);
                } catch (NumberFormatException e) {
                }
            } else if (attributeName.equals(NODE_URI_ATTR)) {
                node.uri = attributeValue;
            }
        }
        if (node.id == 0) {
            throw new XmlPullParserException("Invalid node: " + node.id);
        }

        List<Service> services = new ArrayList<>();
        for (int eventType = parser.nextTag(); !parser.getName().equals(NODE_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(SERVICE_TAG)) {
                Service service = parseService(parser);
                if (service != null) {
                    service.node = node;
                    services.add(service);
                }
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }
        node.services = services;

        parser.require(XmlPullParser.END_TAG, null, NODE_TAG);
        return node;
    }

    private static Service parseService(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, SERVICE_TAG);

        Service service = new Service();
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
            } else if (attributeName.equals(SERVICE_INTERFACE_ATTR)) {
                service.interfaceDescriptor = attributeValue;
            }
        }
        if (service.id == 0 || service.name == null || service.name.isEmpty()
                || service.interfaceDescriptor == null || service.interfaceDescriptor.isEmpty()) {
            throw new XmlPullParserException("Invalid service: " + service.id);
        }

        for (int eventType = parser.nextTag(); !parser.getName().equals(SERVICE_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            String tag = parser.getName();
            skipSubTree(parser);
            parser.require(XmlPullParser.END_TAG, null, tag);
        }

        parser.require(XmlPullParser.END_TAG, null, SERVICE_TAG);
        return service;
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
