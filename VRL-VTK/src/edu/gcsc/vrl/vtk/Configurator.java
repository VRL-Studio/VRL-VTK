/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.io.IOUtil;
import eu.mihosoft.vrl.system.*;
import eu.mihosoft.vrl.visual.ActionDelelator;
import eu.mihosoft.vrl.visual.VAction;
import eu.mihosoft.vrl.visual.VSwingUtil;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import vtk.vtkNativeLibrary;
import vtk.vtkObject;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
public class Configurator extends VPluginConfigurator {

    private File templateProjectSrc;

    public Configurator() {
        setIdentifier(new PluginIdentifier("VRL-VTK", "1.0"));
        setDescription("VTK Integration Plugin");
        exportPackage("edu.gcsc.vrl.vtk");
        exportPackage("vtk");
        exportPackage("eu.mihosoft.vtk");

        addDependency(new PluginDependency("VRL", "0.4.1", "0.4.x"));

        setLoadNativeLibraries(false);
    }

    public void register(PluginAPI api) {
        VPluginAPI vapi = (VPluginAPI) api;

        vapi.addComponent(GridPainter3D.class);
        vapi.addComponent(VTKSampleComponent.class);
        vapi.addComponent(VTUViewer.class);

        vapi.addTypeRepresentation(VTKOutputType.class);

        vapi.addAction(new VAction("VTK-Test") {

            @Override
            public void actionPerformed(ActionEvent e, Object owner) {
                SphereInteractorPanel.main(new String[0]);
            }
        }, ActionDelelator.TOOL_MENU);

        // We make use of VTK GC which will run at 
        VSwingUtil.invokeLater(new Runnable() {

            public void run() {
                vtkObject.JAVA_OBJECT_MANAGER.getAutoGarbageCollector().SetAutoGarbageCollection(true);
                vtkObject.JAVA_OBJECT_MANAGER.getAutoGarbageCollector().SetScheduleTime(1, TimeUnit.MINUTES);
            }
        });
    }

    @Override
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

        //
        String path = getNativeLibFolder().getAbsolutePath();

        try {
            
            System.loadLibrary("jawt");
            SysUtil.loadLibraries(path);

        } catch (Throwable tr) {
            Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, tr);
        }

//        try {
//            VSysUtil.addNativeLibraryPath(path);
//        } catch (IOException ex) {
//            Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        if (!vtkNativeLibrary.LoadAllNativeLibraries()) {
//            for (vtkNativeLibrary lib : vtkNativeLibrary.values()) {
//                if (!lib.IsLoaded()) {
//                    System.out.println(lib.GetLibraryName() + " not loaded");
//                }
//            }
//
//            System.out.println("Make sure the search path is correct: ");
//            System.out.println(System.getProperty("java.library.path"));
//        }
//
//        vtkNativeLibrary.DisableOutputWindow(null);
    }
}
