/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.system.PluginAPI;
import eu.mihosoft.vrl.system.PluginConfigurator;
import eu.mihosoft.vrl.system.PluginDependency;
import eu.mihosoft.vrl.system.PluginIdentifier;
import eu.mihosoft.vrl.system.VPluginAPI;
import eu.mihosoft.vrl.system.VPluginConfigurator;
import java.awt.image.BufferedImage;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Configurator extends VPluginConfigurator{

    public Configurator() {
        setIdentifier(new PluginIdentifier("VRL-VTK", "0.1"));
        setDescription("Simple VTK Viewer");
    }


    public void register(PluginAPI api) {
        VPluginAPI vapi = (VPluginAPI)api;
        vapi.addComponent(GridPainter3D.class);
    }

    public void unregister(PluginAPI api) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void init() {
       //
    }
    
}
