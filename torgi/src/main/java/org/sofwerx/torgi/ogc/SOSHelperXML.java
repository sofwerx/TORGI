package org.sofwerx.torgi.ogc;

import android.content.Context;
import android.location.Location;

import org.sofwerx.torgi.Config;
import org.sofwerx.torgi.gnss.helper.GeoPackageSatDataHelper;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

//Built to comply with OGC SOS v2.0, see http://cite.opengeospatial.org/pub/cite/files/edu/sos/text/main.html
@Deprecated
public class SOSHelperXML {
    private final static SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    public final static String REQUEST_GET_OBSERVATION = "GetObservation";
    public final static String REQUEST_DESCRIBE_SENSOR = "DescribeSensor";
    public final static String REQUEST_GET_CAPABILITIES = "GetCapabilities";

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

    //Using the stringwriter based approach due to some XmlSerializer issue with a specific value in the om:result tag
    public static String getObservation(ArrayList<GeoPackageSatDataHelper> dps) {
        if ((dps == null) || dps.isEmpty())
            return null;
        StringWriter out = new StringWriter();
        out.append("<sos:GetObservationResponse xmlns:sams=\"http://www.opengis.net/samplingSpatial/2.0\">\n");
        for (GeoPackageSatDataHelper dp:dps) {
            out.append(getObservationTag(dp,2));
        }
        out.append("</sos:GetObservationResponse>");

        return out.toString();
    }

    //Using the text based approach due to some issue with a specific value in the om:result tag
    private static String getObservationTag(GeoPackageSatDataHelper dp, int leadSpaces) {
        if (dp == null)
            return null;
        StringWriter leadWriter = new StringWriter();
        if (leadSpaces > 0) {
            for (int i=0;i<leadSpaces;i++)
                leadWriter.append(' ');
        }
        final String lead = leadWriter.toString();
        StringWriter out = new StringWriter();
        out.append(lead+"<sos:observationData>\n");
        out.append(lead+"  <om:OM_Observation gml:id="+dp.getId()+">\n");
        out.append(lead+"    <om:type xlink:href=\"http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement\"/>\n");
        out.append(lead+"    <om:phenomenonTime>\n");
        out.append(lead+"      <gml:TimeInstant gml:id=\"phenomenonTime_"+dp.getId()+"\">\n");
        out.append(lead+"        <gml:timePosition>"+formatTime(dp.getMeassuredTime())+"</gml:timePosition>\n");
        out.append(lead+"      </gml:TimeInstant>\n");
        out.append(lead+"    </om:phenomenonTime>\n");
        out.append(lead+"    <om:resultTime xlink:href=\"#phenomenonTime_"+dp.getId()+"\"/>\n");
        out.append(lead+"    <om:result xmlns:ns=\"http://www.opengis.net/gml/3.2\" uom=\"dB-Hz\" xsi:type=\"ns:MeasureType\">"+Double.toString(dp.getCn0())+"</om:result>\n");
        out.append(lead+"  </om:OM_Observation>\n");
        out.append(lead+"</sos:observationData>\n");
        return out.toString();
    }

    /*public static String getObservationResult(ArrayList<GeoPackageSatDataHelper> dps) {
        if ((dps == null) || dps.isEmpty())
            return null;
        ArrayList<XmlHelper.Tag> dataTags = new ArrayList<>();
        for (GeoPackageSatDataHelper dp:dps) {
            dataTags.add(getObservationTag(dp));
        }
        XmlHelper.Tag[] dataTagsArray = null;
        final int MAX = 1;
        int size = dataTags.size();
        if (size > MAX)
            size = MAX;
        if (dataTags != null) {
            dataTagsArray = new XmlHelper.Tag[size];
            for (int i=0;i<size;i++) {
                dataTagsArray[i] = dataTags.get(i);
            }
        }
        XmlHelper helper = new XmlHelper(new XmlHelper.Tag(
                "sos","GetObservationResponse",null,
                new XmlHelper.Attribute[]{
                        new XmlHelper.Attribute("xmlns","sams","http://www.opengis.net/samplingSpatial/2.0")},
                dataTagsArray));

        return helper.toString();
    }

    //TOGO having a value in the om:result tag is crashing for some reason so trying a different option
    private static XmlHelper.Tag getObservationTag(GeoPackageSatDataHelper dp) {
        if (dp == null)
            return null;
        return new XmlHelper.Tag(
                "sos","observationData",null,null,
                new XmlHelper.Tag[]{
                        new XmlHelper.Tag(
                                "om","OM_Observation",null,
                                new XmlHelper.Attribute[]{
                                        new XmlHelper.Attribute("gml","id",Long.toString(dp.getId()))},
                                new XmlHelper.Tag[]{
                                        new XmlHelper.Tag("om","type",null, new XmlHelper.Attribute[] {new XmlHelper.Attribute("xlink","href","http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement")},null),
                                        new XmlHelper.Tag("om","phenomenonTime",null, null,
                                                new XmlHelper.Tag[]{new XmlHelper.Tag("gml","TimeInstant",null, new XmlHelper.Attribute[]{new XmlHelper.Attribute("gml","id","phenomenonTime_"+Long.toString(dp.getId()))},
                                                        new XmlHelper.Tag[]{new XmlHelper.Tag("gml","timePosition",formatTime(dp.getMeassuredTime()), null, null)})}),
                                        new XmlHelper.Tag("om","resultTime",null, new XmlHelper.Attribute[] {new XmlHelper.Attribute("xlink","href","#phenomenonTime_"+Long.toString(dp.getId()))},null),
                                        new XmlHelper.Tag("om","result", Double.toString(dp.getCn0()),new XmlHelper.Attribute[]{
                                                new XmlHelper.Attribute("xmlns","ns","http://www.opengis.net/gml/3.2"),
                                                new XmlHelper.Attribute(null,"uom","dB-Hz"),
                                                new XmlHelper.Attribute("xsi","type","ns:MeasureType")},null)
                                })
                }
        );
    } */

    private static String formatTime(long time) {
        return dateFormatISO8601.format(time);
    }

    public static String getDescribeSensor(Context context, Location last) {
        StringWriter out = new StringWriter();

        out.append("<swes:DescribeSensorResponse>\n" +
                "  <swes:procedureDescriptionFormat>http://www.opengis.net/sensorML/1.0.1</swes:procedureDescriptionFormat>\n" +
                "  <swes:description>\n" +
                "    <swes:SensorDescription>\n" +
                "      <swes:validTime>\n" +
                "        <gml:TimePeriod>\n" +
                "          <gml:beginPosition>"+formatTime(System.currentTimeMillis())+"</gml:beginPosition>\n" +
                "          <gml:endPosition indeterminatePosition=\"unknown\"/>\n" +
                "        </gml:TimePeriod>\n" +
                "      </swes:validTime>\n" +
                "      <swes:data>\n" +
                "        <sml:SensorML xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\" version=\"1.0.1\">\n" +
                "          <sml:member>\n" +
                "            <sml:System xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:swe=\"http://www.opengis.net/swe/1.0.1\" xmlns:sos=\"http://www.opengis.net/sos/2.0\">\n" +
                "              <sml:identification>\n" +
                "                <sml:IdentifierList>\n" +
                "                  <sml:identifier name=\"uniqueID\">\n" +
                "                    <sml:Term definition=\"urn:ogc:def:identifier:OGC:1.0:uniqueID\">\n" +
                "                      <sml:value>"+ Config.getInstance(context).getUuid()+"</sml:value>\n" +
                "                    </sml:Term>\n" +
                "                  </sml:identifier>\n" +
                "                  <sml:identifier name=\"longName\">\n" +
                "                    <sml:Term definition=\"urn:ogc:def:identifier:OGC:1.0:longName\">\n" +
                "                      <sml:value>TORGI instance "+Config.getInstance(context).getUuid()+"</sml:value>\n" +
                "                    </sml:Term>\n" +
                "                  </sml:identifier>\n" +
                "                  <sml:identifier name=\"shortName\">\n" +
                "                    <sml:Term definition=\"urn:ogc:def:identifier:OGC:1.0:shortName\">\n" +
                "                      <sml:value>TORGI</sml:value>\n" +
                "                    </sml:Term>\n" +
                "                  </sml:identifier>\n" +
                "                </sml:IdentifierList>\n" +
                "              </sml:identification>\n" +
                "              <sml:validTime>\n" +
                "                <gml:TimePeriod>\n" +
                "                  <gml:beginPosition>"+formatTime(System.currentTimeMillis())+"</gml:beginPosition>\n" +
                "                  <gml:endPosition indeterminatePosition=\"unknown\"/>\n" +
                "                </gml:TimePeriod>\n" +
                "              </sml:validTime>\n");
        if (last != null) {
            out.append("              <sml:position name=\"sensorPosition\">\n" +
                        "                <swe:Position referenceFrame=\"urn:ogc:def:crs:EPSG::4326\">\n" +
                        "                  <swe:location>\n" +
                        "                    <swe:Vector gml:id=\"STATION_LOCATION\">\n" +
                        "                      <swe:coordinate name=\"easting\">\n" +
                        "                        <swe:Quantity axisID=\"x\">\n" +
                        "                          <swe:uom code=\"degree\"/>\n" +
                        "                          <swe:value>"+Double.toString(last.getLongitude())+"</swe:value>\n" +
                        "                        </swe:Quantity>\n" +
                        "                      </swe:coordinate>\n" +
                        "                      <swe:coordinate name=\"northing\">\n" +
                        "                        <swe:Quantity axisID=\"y\">\n" +
                        "                          <swe:uom code=\"degree\"/>\n" +
                        "                          <swe:value>"+Double.toString(last.getLatitude())+"</swe:value>\n" +
                        "                        </swe:Quantity>\n" +
                        "                      </swe:coordinate>\n");
                if (last.hasAltitude()) {
                    out.append("                      <swe:coordinate name=\"altitude\">\n" +
                                "                        <swe:Quantity axisID=\"z\">\n" +
                                "                          <swe:uom code=\"m\"/>\n" +
                                "                          <swe:value>52.0</swe:value>\n" +
                                "                        </swe:Quantity>\n" +
                                "                      </swe:coordinate>\n");
                }
            }
            out.append("                    </swe:Vector>\n" +
                        "                  </swe:location>\n" +
                        "                </swe:Position>\n" +
                        "              </sml:position>\n");
            out.append("            </sml:System>\n" +
                        "          </sml:member>\n" +
                        "        </sml:SensorML>\n" +
                        "      </swes:data>\n" +
                        "    </swes:SensorDescription>\n" +
                        "  </swes:description>\n" +
                        "</swes:DescribeSensorResponse>");

        return out.toString();
    }
}
