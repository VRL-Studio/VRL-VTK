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
import java.awt.image.BufferedImage;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Configurator implements PluginConfigurator{

    public void register(PluginAPI api) {
        VPluginAPI vapi = (VPluginAPI)api;
        vapi.addComponent(GridPainter3D.class);
    }

    public void unregister(PluginAPI api) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getDescription() {
        return "Simple VTK Viewer";
    }

    public BufferedImage getIcon() {
        return null;
    }

    public PluginIdentifier getIdentifier() {
        return new PluginIdentifier("VRL-VTK", "0.1");
    }

    public void init() {
        //
    }

    public PluginDependency[] getDependencies() {
        return new PluginDependency[0];
    }
    
}
