/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.TypeInfo;
import eu.mihosoft.vrl.reflection.TypeRepresentationBase;
import eu.mihosoft.vrl.visual.ResizableContainer;
import eu.mihosoft.vrl.visual.VContainer;
import eu.mihosoft.vrl.visual.VSwingUtil;
import eu.mihosoft.vtk.VTKJPanel;
import groovy.lang.Script;
import java.awt.Color;
import java.awt.Dimension;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
@TypeInfo(type = Visualization.class, input = false, output = true, style = "default")
public class VTKOutputType extends TypeRepresentationBase {

    private Visualization viewValue;
//    private VTKView view;
    private VTKJPanel view;
//    private JFrame frame;
    private static int NUMBER_OF_INSTANCES;
    private static int MAX_NUMBER_OF_INSTANCES = 16;

    public VTKOutputType() {

        NUMBER_OF_INSTANCES++;

        if (NUMBER_OF_INSTANCES > MAX_NUMBER_OF_INSTANCES) {
            NUMBER_OF_INSTANCES--;
            throw new IllegalStateException(
                    getClass()
                    + " only supports up to "
                    + MAX_NUMBER_OF_INSTANCES
                    + " number of instances!");
        }

        VSwingUtil.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                initialize();
            }
        });
    }

    private void initialize() {
//        view = new VTKView(this);

//        view = new VTKJPanel();

        view = new VTKCanvas3D(this);

        view.setMinimumSize(new Dimension(160, 120));
        view.setMaximumSize(new Dimension(600, 600));
        view.getRenderer().GetActiveCamera().Dolly(0.2);
        view.setOpaque(false);

        VContainer cont = new VContainer();
        cont.add(view);
        
        add(cont);

        cont.setMinimumSize(new Dimension(160, 120));
        cont.setPreferredSize(new Dimension(160, 120));
        cont.setMaximumSize(new Dimension(600, 600));
        
        setValueOptions("width=160;height=120;blurValue=0.7F;");
        
        this.setInputComponent(cont);
    }

    @Override
    public void setViewValue(final Object o) {

        VSwingUtil.invokeAndWait(new Runnable() {

            public void run() {
                if (!(o instanceof Visualization)) {
                    return;
                }

                final Visualization v = (Visualization) o;
                v.registerWithRenderer(view.getRenderer());

                viewValue = v;

                view.setBackground(v.getBackground());
                view.contentChanged();
                view.repaint();
            }
        });


    }

    @Override
    public Object getViewValue() {
        return viewValue;
    }

    @Override
    public void emptyView() {

        super.emptyView();

        VSwingUtil.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (viewValue != null) {
                    viewValue.unregisterFromRenderer(view.getRenderer());
                }
            }
        });

        viewValue = null;
    }
    
    /**
     * Defines the Vcanvas3D size by evaluating a groovy script.
     * @param script the script to evaluate
     */
    private void setVCanvas3DSizeFromValueOptions(Script script) {
        Integer w = null;
        Integer h = null;
        Object property = null;

        if (getValueOptions() != null) {

            if (getValueOptions().contains("width")) {
                property = script.getProperty("width");
            }

            if (property != null) {
                w = (Integer) property;
            }

            property = null;

            if (getValueOptions().contains("height")) {
                property = script.getProperty("height");
            }

            if (property != null) {
                h = (Integer) property;
            }
        }

        if (w != null && h != null) {
            // TODO find out why offset is 10
            view.setPreferredSize(new Dimension(w - 10, h));
            view.setSize(new Dimension(w - 10, h));
        }

        System.out.println(getValueOptions());
    }
    
     @Override
    protected void evaluationRequest(Script script) {
        setVCanvas3DSizeFromValueOptions(script);
    }

    @Override
    public void dispose() {

        super.dispose();

        if (view != null) {
            view.dispose();
            view = null;
            NUMBER_OF_INSTANCES--;
        }
    }
}
