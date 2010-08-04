/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import eu.mihosoft.vrl.io.Base64;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class DataArray {

    private byte[] data;
    private HashMap<String, String> attributes = new HashMap<String, String>();
    private int numberOfComponents = 1;
    private String type = "?";
    private String name = "?";
    private DataDecoder dataDecoder = new DataDecoder();

    public DataArray() {
    }

    public DataArray(Node n, DecoderFactory decoderFactory) {


        for (int j = 0; j < n.getAttributes().getLength(); j++) {
            Node a = n.getAttributes().item(j);
//            System.out.println("Attribute " + j + " : " + a);

            attributes.put(a.getNodeName(), a.getNodeValue());
        }
        
        
        if (attributes.get("NumberOfComponents") != null) {
            numberOfComponents =
                    new Integer(attributes.get("NumberOfComponents"));
        }

        type = attributes.get("type").trim();

        if (attributes.get("Name") != null) {
            name = attributes.get("Name").trim();
        }

        System.out.println("DataArray: " + name);
        System.out.println(">> type: " + type);

        if (attributes.get("format").equals("binary".trim())) {
            System.out.println(">> format: binary");

            // each Base64 entry consists of two entries
            // we only use the second one to get rid of the data length
            // int32 header
            String dataString = n.getTextContent().split("==")[1];

            data = Base64.decode(dataString);

            System.out.println(">> BASE64 String length: " + dataString.length());
            System.out.println(">> array size: " + data.length / 4);



        } else {
            // don't know what to do...
        }


        dataDecoder = decoderFactory.getDecoder(type);

        try {
            dataDecoder.decode(data);
        } catch (IOException ex) {
            Logger.getLogger(DataArray.class.getName()).
                    log(Level.SEVERE, null, ex);
        } catch (UnsupportedArrayTypeException ex) {
            Logger.getLogger(DataArray.class.getName()).
                    log(Level.SEVERE, null, ex);
        }

    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * @return the attributes
     */
    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * @return the numberOfComponents
     */
    public int getNumberOfComponents() {
        return numberOfComponents;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the dataDecoder
     */
    public DataDecoder getDataDecoder() {
        return dataDecoder;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
}
