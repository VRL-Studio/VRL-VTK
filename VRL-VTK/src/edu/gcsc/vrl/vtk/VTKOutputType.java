/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.TypeInfo;
import eu.mihosoft.vrl.reflection.TypeRepresentationBase;
import eu.mihosoft.vrl.visual.ResizableContainer;
import eu.mihosoft.vrl.visual.VSwingUtil;
import eu.mihosoft.vtk.VTKJPanel;
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

    public VTKOutputType() {

        VSwingUtil.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                initialize();
            }
        });
    }

    private void initialize() {
//        view = new VTKView(this);

        view = new VTKJPanel();
        view.setMinimumSize(new Dimension(300, 200));
        view.getRenderer().GetActiveCamera().Dolly(0.2);
        view.setOpaque(false);

        ResizableContainer cont = new ResizableContainer(view, this);
        add(cont);

        cont.setMinimumSize(new Dimension(300, 200));
        cont.setPreferredSize(new Dimension(300, 200));
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

    @Override
    public void dispose() {
        super.dispose();

        if (view != null) {
            view.dispose();
        }
    }
}
