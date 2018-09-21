package org.sofwerx.torgi.ogc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//Built to comply with OGC SOS v2.0, see http://cite.opengeospatial.org/pub/cite/files/edu/sos/text/main.html
public class SOSHelper {
    public static String getCapabilities() {
        XmlHelper helper = new XmlHelper(new XmlHelper.Tag(
                "sos", "Capabilities", null,
                new XmlHelper.Attribute[]{
                    new XmlHelper.Attribute("xmlns","sos","http://www.opengis.net/sos/2.0"),
                    new XmlHelper.Attribute("xmlns","xsi","http://www.w3.org/2001/XMLSchema-instance"),
                    new XmlHelper.Attribute("xmlns","xlink","http://www.w3.org/1999/xlink"),
                    new XmlHelper.Attribute("xmlns","fes","http://www.opengis.net/fes/2.0"),
                    new XmlHelper.Attribute("xmlns","swes","http://www.opengis.net/swes/2.0"),
                    new XmlHelper.Attribute("xmlns","gml","http://www.opengis.net/gml/3.2"),
                    new XmlHelper.Attribute(null,"version","2.0.0"),
                    new XmlHelper.Attribute("xmlns","ows","http://www.opengis.net/ows/1.1"),
                    new XmlHelper.Attribute("xis","schemaLocation","http://www.opengis.net/fes/2.0 http://schemas.opengis.net/filter/2.0/filterAll.xsd http://www.opengis.net/swes/2.0 http://schemas.opengis.net/swes/2.0/swes.xsd http://www.opengis.net/sos/2.0 http://schemas.opengis.net/sos/2.0/sosGetCapabilities.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd http://www.opengis.net/ows/1.1 http://schemas.opengis.net/ows/1.1.0/owsAll.xsd"),},
                new XmlHelper.Tag[]{
                    new XmlHelper.Tag("ows","ServiceIdentification",null,null,null),
                    new XmlHelper.Tag("ows","ServiceProvider",null,null,null),
                    new XmlHelper.Tag("ows","OperationsMetadata",null,null,null),
                    new XmlHelper.Tag("sos","extension",null,null,null),
                    new XmlHelper.Tag("sos","filterCapabilities",null,null,null),
                    new XmlHelper.Tag("sos","contents",null,null,null)
                }));

        return helper.toString();
    }

    //TODO respond to DescribeSensor
    //TODO respond to GetObservation
}
