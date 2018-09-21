package org.sofwerx.torgi.ogc;

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class XmlHelper {
    private XmlSerializer xmlS;
    private Tag root;
    public XmlHelper(Tag root) {
        xmlS = Xml.newSerializer();
        this.root = root;
    }

    public String toString() {
        if ((root != null) && (xmlS != null)) {
            try {
                StringWriter writer = new StringWriter();
                xmlS.setOutput(writer);
                xmlS.startDocument("UTF-8", false);
                root.build(xmlS);
                xmlS.endDocument();
                return writer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static class Tag {
        private String namespace = null;
        private String name = null;
        private ArrayList<Attribute> attributes = null;
        private String text = null;
        private ArrayList<Tag> tags = null;

        public Tag(String namespace, String name, String text, Attribute[] attributes, Tag[] tags) {
            this.namespace = namespace;
            this.name = name;
            this.text = text;
            if (attributes != null)
                this.attributes = new ArrayList<>(Arrays.asList(attributes));
            if (tags != null)
                this.tags = new ArrayList<>(Arrays.asList(tags));
        }

        public void add(Attribute attribute) {
            if (attribute != null) {
                if (attributes == null)
                    attributes = new ArrayList<>();
                attributes.add(attribute);
            }
        }

        public void add(Tag tag) {
            if (tag != null) {
                if (tags == null)
                    tags = new ArrayList<>();
                tags.add(tag);
            }
        }

        public void setText(String text) {
            this.text = text;
        }

        protected void build(XmlSerializer serializer) throws IOException {
            if (serializer != null) {
                String tagText;
                if (namespace == null)
                    tagText = name;
                else
                    tagText = namespace+":"+name;
                serializer.startTag(null,tagText);
                if (text != null)
                    serializer.text(text);
                if ((attributes != null) && !attributes.isEmpty()) {
                    for (Attribute attribute:attributes) {
                        if (attribute.namespace == null)
                            serializer.attribute(null,attribute.name,attribute.value);
                        else
                            serializer.attribute(null,attribute.namespace+":"+attribute.name,attribute.value);
                    }
                }
                if ((tags != null) && !tags.isEmpty()) {
                    for (Tag tag:tags) {
                        tag.build(serializer);
                    }
                }
                serializer.endTag(null,tagText);
            }
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Attribute {
        private String namespace = null;
        private String name = null;
        private String value = null;

        public Attribute(String namespace, String name, String value) {
            this.namespace = namespace;
            this.name = name;
            this.value = value;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
