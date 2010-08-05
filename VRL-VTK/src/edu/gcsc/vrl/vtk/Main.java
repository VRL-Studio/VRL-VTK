package edu.gcsc.vrl.vtk;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import eu.mihosoft.vrl.animation.LinearTarget;
import eu.mihosoft.vrl.io.Base64;
import eu.mihosoft.vrl.visual.ImageUtils;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        UnstructuredGrid grid =
                new UnstructuredGrid(
                new File(
//                "/Users/miho/EigeneApps/VRL-VTK/vtk-test-files/finiteout.000045_0000.vtu"));
//                "/Users/miho/EigeneApps/VRL-VTK/vtk-test-files/finiteout.000132_0000.vtu"));
        "/Users/miho/EigeneApps/VRL-VTK/vtk-test-files/finiteout.000408_0000.vtu"));
//                "/Users/miho/EigeneApps/VRL-VTK/vtk-test-files/finiteout.000777_0000.vtu"));
//                "/Users/miho/EigeneApps/VRL-VTK/vtk-test-files/finiteout.001179_0000.vtu"));

        GridPainter p = new GridPainter();

        p.setFlipY(true);
//        p.setOffset(new Point2D.Double(0, 512));

        BufferedImage img = p.paint(2048, 2048, grid);

        try {
            ImageIO.write(img, "png", new File("out.png"));
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
