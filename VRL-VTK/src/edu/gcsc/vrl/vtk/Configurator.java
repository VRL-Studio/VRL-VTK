/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.system.*;
import java.awt.image.BufferedImage;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Configurator extends VPluginConfigurator{

    public Configurator() {
        setIdentifier(new PluginIdentifier("VRL-VTK", "0.2"));
        setDescription("Simple VTK Viewer");
    }

    public void register(PluginAPI api) {
        VPluginAPI vapi = (VPluginAPI)api;
        vapi.addComponent(GridPainter3D.class);
    }

    public void unregister(PluginAPI api) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void init(InitPluginAPI iApi) {
       //
    }
    
}
