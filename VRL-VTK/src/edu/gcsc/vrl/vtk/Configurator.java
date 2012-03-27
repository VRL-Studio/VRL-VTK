/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.io.IOUtil;
import eu.mihosoft.vrl.system.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Configurator extends VPluginConfigurator{
    
    private File templateProjectSrc;

    public Configurator() {
        setIdentifier(new PluginIdentifier("VRL-VTK", "0.2"));
        setDescription("Simple VTK Viewer");
        exportPackage("edu.gcsc.vrl.vtk");
    }

    public void register(PluginAPI api) {
        VPluginAPI vapi = (VPluginAPI)api;
        vapi.addComponent(GridPainter3D.class);
    }

    public void unregister(PluginAPI api) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void init(InitPluginAPI iApi) {
       templateProjectSrc = new File(iApi.getResourceFolder(), "vtk-template01.vrlp");

        if (!templateProjectSrc.exists()) {
            InputStream in = Configurator.class.getResourceAsStream(
                    "/edu/gcsc/vrl/vtk/resources/vtk-template01.vrlp");
            try {
                IOUtil.saveStreamToFile(in, templateProjectSrc);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Configurator.class.getName()).
                        log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Configurator.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }

        iApi.addProjectTemplate(new ProjectTemplate() {

            public String getName() {
                return "VTK - Project";
            }

            public File getSource() {
                return templateProjectSrc;
            }

            public String getDescription() {
                return "Basic VTK Project";
            }

            public BufferedImage getIcon() {
                return null;
            }
        });
    }
    
}
