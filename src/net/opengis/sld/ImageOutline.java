//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.03.07 at 11:54:57 AM IST 
//


package net.opengis.sld;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element ref="{http://www.opengis.net/sld}LineSymbolizer"/>
 *         &lt;element ref="{http://www.opengis.net/sld}PolygonSymbolizer"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "lineSymbolizer",
    "polygonSymbolizer"
})
@XmlRootElement(name = "ImageOutline")
public class ImageOutline {

    @XmlElement(name = "LineSymbolizer")
    protected LineSymbolizer lineSymbolizer;
    @XmlElement(name = "PolygonSymbolizer")
    protected PolygonSymbolizer polygonSymbolizer;

    /**
     * Gets the value of the lineSymbolizer property.
     * 
     * @return
     *     possible object is
     *     {@link LineSymbolizer }
     *     
     */
    public LineSymbolizer getLineSymbolizer() {
        return lineSymbolizer;
    }

    /**
     * Sets the value of the lineSymbolizer property.
     * 
     * @param value
     *     allowed object is
     *     {@link LineSymbolizer }
     *     
     */
    public void setLineSymbolizer(LineSymbolizer value) {
        this.lineSymbolizer = value;
    }

    /**
     * Gets the value of the polygonSymbolizer property.
     * 
     * @return
     *     possible object is
     *     {@link PolygonSymbolizer }
     *     
     */
    public PolygonSymbolizer getPolygonSymbolizer() {
        return polygonSymbolizer;
    }

    /**
     * Sets the value of the polygonSymbolizer property.
     * 
     * @param value
     *     allowed object is
     *     {@link PolygonSymbolizer }
     *     
     */
    public void setPolygonSymbolizer(PolygonSymbolizer value) {
        this.polygonSymbolizer = value;
    }

}
