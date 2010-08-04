package edu.gcsc.vrl.vtk;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



import java.io.File;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class UnstructuredGrid {
    private ArrayList<DataArray> arrays = new ArrayList<DataArray>();

    public UnstructuredGrid() {
    }

   public UnstructuredGrid(File file) {
       DecoderFactory decoderFactory = new DecoderFactory();

        try {
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println("Root element " +
                    doc.getDocumentElement().getNodeName());
            NodeList nodeLst = doc.getElementsByTagName("UnstructuredGrid");
//            System.out.println("Information of all Nodes");

            for (int s = 0; s < nodeLst.getLength(); s++) {

                Node fstNode = nodeLst.item(s);

                if (fstNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element fstElmnt = (Element) fstNode;
                    NodeList fstNmElmntLst =
                            fstElmnt.getElementsByTagName("DataArray");

                    for (int i = 0; i < fstNmElmntLst.getLength(); i++) {
                        Node n = fstNmElmntLst.item(i);
//                        System.out.println("Node " + i + " : " + n);

                        DataArray array = new DataArray(n, decoderFactory);

//                        if (array.getDataDecoder() instanceof Float32Decoder) {
//
//                            float[] data = (float[]) array.getDataDecoder().getArray();
//
//                            for (int j = 0; j < data.length; j++) {
//                                float f = data[j];
//                                System.out.println("F: " + f);
//                            }
//
//                        } else if (array.getDataDecoder() instanceof Int32Decoder) {
//                            int[] data = (int[]) array.getDataDecoder().getArray();
//
//                            for (int j = 0; j < data.length; j++) {
//                                int k = data[j];
//                                System.out.println("I: " + k);
//                            }
//                        } else if (array.getDataDecoder() instanceof Int8Decoder) {
//                            int[] data = (int[]) array.getDataDecoder().getArray();
//
//                            for (int j = 0; j < data.length; j++) {
//                                int k = data[j];
//                                System.out.println("I8: " + k);
//                            }
//                        }

                        arrays.add(array);
                    }


//                    Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
//                    System.out.println(fstNmElmnt);
//                    NodeList fstNm = fstNmElmnt.getChildNodes();
//
//                    for(int i = 0 ; i < fstNm.getLength();i++) {
//                        Node n = fstNm.item(i);
//                        System.out.println("Node "+ i + " : " + n);
//                    }




//                    Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
//                    NodeList fstNm = fstNmElmnt.getChildNodes();
//                    System.out.println("First Name : " + ((Node) fstNm.item(0)).getNodeValue());
//                    NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("lastname");
//                    Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
//                    NodeList lstNm = lstNmElmnt.getChildNodes();
//                    System.out.println("Last Name : " + ((Node) lstNm.item(0)).getNodeValue());
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the arrays
     */
    public ArrayList<DataArray> getArrays() {
        return arrays;
    }

}
