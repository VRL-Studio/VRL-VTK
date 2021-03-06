package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.annotation.ComponentInfo;
import eu.mihosoft.vrl.annotation.MethodInfo;
import eu.mihosoft.vrl.annotation.OutputInfo;
import eu.mihosoft.vrl.annotation.ParamGroupInfo;
import eu.mihosoft.vrl.annotation.ParamInfo;
import eu.mihosoft.vrl.io.VPropertyFolderManager;
import eu.mihosoft.vrl.reflection.DefaultMethodRepresentation;
import eu.mihosoft.vrl.reflection.TypeRepresentationBase;
import eu.mihosoft.vrl.reflection.VisualCanvas;
import eu.mihosoft.vrl.reflection.VisualObject;
import eu.mihosoft.vrl.system.VMessage;
import eu.mihosoft.vrl.system.VRL;
import eu.mihosoft.vrl.types.MethodRequest;
import eu.mihosoft.vrl.types.MultipleOutputType;
import eu.mihosoft.vrl.types.SelectionInputType;
import eu.mihosoft.vrl.types.observe.LoadFileObservable;
import eu.mihosoft.vrl.types.observe.LoadObserveFileType;
import eu.mihosoft.vrl.types.observe.VTypeObserveUtil;
import eu.mihosoft.vrl.visual.VSwingUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import vtk.vtkActor;
import vtk.vtkArrowSource;
import vtk.vtkCellCenters;
import vtk.vtkCellData;
import vtk.vtkCellDataToPointData;
import vtk.vtkColorTransferFunction;
import vtk.vtkContourFilter;
import vtk.vtkDataArray;
import vtk.vtkDataSetMapper;
import vtk.vtkGlyph3D;
import vtk.vtkLookupTable;
import vtk.vtkMapper;
import vtk.vtkOutlineFilter;
import vtk.vtkPlaneSource;
import vtk.vtkPointData;
import vtk.vtkPolyDataMapper;
import vtk.vtkProperty;
import vtk.vtkUnstructuredGrid;
import vtk.vtkWarpScalar;
import vtk.vtkXMLUnstructuredGridReader;

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 * @author Andreas Vogel <andreas.vogel@gcsc.uni-frankfurt.de>
 * @author Christian Poliwoda <christian.poliwoda@gcsc.uni-frankfurt.de>
 */
@ComponentInfo(name = "VTUViewer", category = "VTK")
public class VTUViewer implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static class Range {

        private static final String AUTO = "Auto";
        private static final String MIN_MAX = "Min/Max";
    }

    public static class Arrow {

        private static final String AUTO = "Auto";
        private static final String SCALED = "Scaled";
    }

    public static class DisplayStyle {

        private static final String SURFACE = "Surface";
        private static final String SURFACE_EDGE = "Surface/Edge";
        private static final String WIREFRAME = "Wireframe";
        private static final String POINTS = "Points";
        private static final String VECTORFIELD = "Vectorfield";
    }

    public static class DataStyle {

        private static final String NONE = "None";
        private static final String WARP_AUTO = "Warp (Auto)";
        private static final String WARP_FACTOR = "Warp (Factor)";
        private static final String CONTOUR = "Contour";
    }

    public static enum DataType {

        INVALID,
        POINT,
        CELL
    }
    private transient Visualization lastVisualization = new Visualization();
    private transient File lastFile = null;
    private transient Thread thread = null;
    private transient ArrayList<File> lastAllFiles = new ArrayList<File>();
    private transient DefaultMethodRepresentation mVisualizeMethodRep;

    private Thread getThread() {
        synchronized (this) {
            return thread;
        }
    }

    private void setThread(Thread thread) {
        synchronized (this) {
            this.thread = thread;
        }
    }

    private void startThread() {
        synchronized (this) {
            if (this.thread != null) {
                this.thread.start();
            }
        }
    }

    private void interruptThread() {
        synchronized (this) {
            if (this.thread != null) {
                this.thread.interrupt();
            }
        }
    }

    static private class PlotSetup implements Serializable {

        private static final long serialVersionUID = 1L;

        public PlotSetup(String title, String sRange,
                double minValueRange, double maxValueRange,
                boolean bShowLegend, boolean bShowOutline, boolean showOrientation,
                String sDataStyle, String sDisplayStyle,
                double warpFactor, int numContours, double fieldScaleFactor, long wait) {
            this.title = title;
            //
            this.sRange = sRange;
            this.minValueRange = minValueRange;
            this.maxValueRange = maxValueRange;
            //
            this.bShowLegend = bShowLegend;
            this.bShowOutline = bShowOutline;
            this.showOrientation = showOrientation;
            //
            this.sDataStyle = sDataStyle;
            this.sDisplayStyle = sDisplayStyle;
            //
            this.wait = wait;
            //
            this.warpFactor = warpFactor;
            this.numContours = numContours;
            this.fieldScaleFactor = fieldScaleFactor;
        }

        public PlotSetup() {
        }
        //
        public String title = "";
        //
        public String sRange = VTUViewer.Range.AUTO;
        public Double minValueRange = 0.0;
        public Double maxValueRange = 1.0;
        //
        public Boolean bShowLegend = true;
        public Boolean bShowOutline = false;
        public Boolean showOrientation = false;
        //
        public String sDisplayStyle = VTUViewer.DisplayStyle.SURFACE;
        public String sDataStyle = VTUViewer.DataStyle.NONE;
        //
        public Double warpFactor = 1.0;
        public Integer numContours = 5;
        public String arrowStyle = VTUViewer.Arrow.AUTO;
        public Double fieldScaleFactor = 0.05;
        //
        public Long wait = 0L;
        public Boolean makePNG = false;
        //
        // non - stored data
        //
        transient public File fileOrFolder = null;
        transient public String startsWith = "";
        //
        public String elementInFile = "";
        transient public Integer index = 0;
        transient public DataType dataType = DataType.INVALID;
        transient public vtkDataArray dataArray = null;
    }

    private PlotSetup plotSetup = new PlotSetup();

    private class VisRunnable implements Runnable {

        transient protected PlotSetup plotSetup = null;

        VisRunnable(PlotSetup plotSetup) {
            this.plotSetup = plotSetup;
        }

        @Override
        public void run() {
            try {

                final VTUAnalyzer analyzer = (VTUAnalyzer) VTypeObserveUtil.getFileAnanlyzerByClass(VTUAnalyzer.class);
                analyzer.setStartsWith(plotSetup.startsWith);

                final int id = mVisualizeMethodRep.getParentObject().getObjectID();
                final Object o = ((VisualCanvas) mVisualizeMethodRep.getMainCanvas()).getInspector().getObject(id);
                final int windowID = 0;
                final String tag = "element";

                boolean filesAnalysed = false;
                final int initWaitingTime = 10;
                final int waitingTimeIncreaseFactor = 2;
                final int maxWaitingTime = 1000;
                int waitingTime = initWaitingTime;

                while (true) {
                    
                    ArrayList<File> allFiles = getAllFilesInFolder(plotSetup.fileOrFolder, plotSetup.startsWith, "vtu");

                    // if nothing changed, wait 1s before next lookup
                    if (lastAllFiles.size() == allFiles.size()) {
                        if (lastAllFiles.equals(allFiles)) {
                            Thread.sleep(waitingTime);
                            waitingTime *= waitingTimeIncreaseFactor;
                            if (waitingTime > maxWaitingTime) {
                                waitingTime = maxWaitingTime;
                            }
                            continue;
                        }
                    }
                    waitingTime = initWaitingTime;

                    if (!filesAnalysed && !allFiles.isEmpty()) {
                        File file = allFiles.get(0);
                        if (!file.getAbsolutePath().isEmpty() && file.exists()) {

                            if (!file.equals(LoadFileObservable.getInstance().getSelectedFile(tag, o, windowID))) {
                                LoadFileObservable.getInstance().setSelectedFile(file, tag, o, windowID);
                            }

                            ArrayList<String> list = new ArrayList<String>();
                            list.addAll(analyzer.analyzeFile(file));
                            if (!list.isEmpty()) {
                                if (plotSetup.elementInFile.equals("")) {
                                    plotSetup.elementInFile = list.get(0);
                                }
                                plotSetup.index = list.indexOf(plotSetup.elementInFile);
                            }
                        } else {
                            LoadFileObservable.getInstance().setInvalidFile(tag, o, windowID);
                        }

                        filesAnalysed = true;
                    }

                    for (File file : allFiles) {

                        if (lastAllFiles.contains(file)) {
                            continue;
                        }

                        Visualization visualization = createVisualization(file, plotSetup);

                        if (visualization == null) {
                            continue;
                        }

                        lastVisualization = visualization;
                        lastFile = file;

                        SwingUtilities.invokeAndWait(new Runnable() {

                            public void run() {

                                VTKOutputType vtkOutput = ((VTKOutputType) (((MultipleOutputType) mVisualizeMethodRep.getReturnValue()).getTypeContainers().get(0).getTypeRepresentation()));
                                vtkOutput.emptyView();
                                vtkOutput.setViewValue(lastVisualization);

                                if (plotSetup.makePNG) {
                                    String[] split = lastFile.getAbsolutePath().split(".vtu");
                                    File pngFile = new File(split[0] + ".png");
                                    vtkOutput.saveImage(pngFile);
                                }
                            }
                        });

                        Thread.sleep(plotSetup.wait + 1);
                    } // end for files

                    lastAllFiles = allFiles;

                    // if plotting only a single file, exit here
                    if (plotSetup.fileOrFolder != null && !plotSetup.fileOrFolder.isDirectory()) {
                        break;
                    }

                } // end while
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    @OutputInfo(style = "multi-out",
    elemStyles = {"default", "silent"}, elemNames = {"", "File"},
    elemTypes = {Visualization.class, File.class})
    @MethodInfo(hide = false, hideCloseIcon = true, valueStyle = "multi-out", valueOptions = "serialization=false")
    public Object[] visualize(
            MethodRequest mReq,
            @ParamGroupInfo(group = "Files|true|File depending data.")
            @ParamInfo(name = "Folder or File",
            style = "observe-save-dialog",
            options = "tag=\"element\"") final File fileOrFolder,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "File Beginning",
            nullIsValid = true) final String startsWith,
            @ParamGroupInfo(group = "Files")
            @ParamInfo(name = "Produce PNG", options = "value=false")
            final boolean makePNG) {

        mVisualizeMethodRep = mReq.getMethod();

        // store plot setup
        if (plotSetup == null) {
            plotSetup = new PlotSetup();
        }
        plotSetup.fileOrFolder = fileOrFolder;
        plotSetup.startsWith = startsWith;
        plotSetup.makePNG = makePNG;

        // clear
        lastVisualization = new Visualization();
        lastAllFiles = new ArrayList<File>();

        // stop current thread
        interruptThread();

        // start new thread
        setThread(new Thread(new VisRunnable(plotSetup)));
        VRL.getCurrentProjectController().addSessionThread(getThread());
        startThread();

        // determinte file path
        File outFile;
        if (fileOrFolder.isDirectory()) {
            outFile = new File(fileOrFolder, startsWith + ".vtu");

        } else {
            outFile = fileOrFolder;
        }

        //return lastVisualization;
        return new Object[]{lastVisualization, outFile};
    }

    @MethodInfo(hide = false, hideCloseIcon = true, interactive = false)
    public void setup(
            MethodRequest mReq,
            @ParamGroupInfo(group = "Data")
            @ParamInfo(name = "Data Component",
            style = "observe-load-dialog",
            options = "invokeOnChange=true; fileAnalyzer=\"VTUAnalyzer\";tag=\"element\"")
            final String elementInFile,
            @ParamGroupInfo(group = "Data")
            @ParamInfo(name = "Frame Duration (in ms)", options = "invokeOnChange=true; value=0")
            final long wait,
            //
            @ParamGroupInfo(group = "Plot|false|Plot depending data.")
            @ParamInfo(name = "Title", style = "default", options = "invokeOnChange=true")
            final String title,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Range",
            style = "selection",
            options = "invokeOnChange=true; value=[\""
            + VTUViewer.Range.AUTO + "\",\""
            + VTUViewer.Range.MIN_MAX + "\"]") final String sRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Min",
            style = "default",
            options = "invokeOnChange=true; value=0",
            nullIsValid = true) final double minValueRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Max",
            style = "default",
            options = "invokeOnChange=true; value=1",
            nullIsValid = true) final double maxValueRange,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Data Legend", style = "default", options = "invokeOnChange=true; value=true")
            final boolean bShowLegend,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Outline", style = "default", options = "invokeOnChange=true; value=false")
            final boolean bShowOutline,
            @ParamGroupInfo(group = "Plot")
            @ParamInfo(name = "Show Orientation", style = "default", options = "invokeOnChange=true")
            final boolean showOrientation,
            //
            @ParamGroupInfo(group = "Filters|false|Choose which filter should be used.")
            @ParamInfo(name = "Display Style", style = "selection",
            options = "invokeOnChange=true; value=[\""
            + VTUViewer.DisplayStyle.SURFACE + "\",\""
            + VTUViewer.DisplayStyle.SURFACE_EDGE + "\",\""
            + VTUViewer.DisplayStyle.WIREFRAME + "\",\""
            + VTUViewer.DisplayStyle.POINTS + "\",\""
            + VTUViewer.DisplayStyle.VECTORFIELD + "\"]")
            final String sDisplayStyle,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Data Filter", style = "selection",
            options = "invokeOnChange=true; value=[\""
            + VTUViewer.DataStyle.NONE + "\",\""
            + VTUViewer.DataStyle.WARP_AUTO + "\",\""
            + VTUViewer.DataStyle.WARP_FACTOR + "\",\""
            + VTUViewer.DataStyle.CONTOUR + "\"]")
            final String sDataStyle,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Warp Factor",
            style = "default",
            options = "invokeOnChange=true; value=1",
            nullIsValid = true) final double warpFactor,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Num Contour",
            style = "default",
            options = "invokeOnChange=true; value=5",
            nullIsValid = true) final int numContours,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Arrow Size",
            style = "selection",
            options = "invokeOnChange=true; value=[\""
            + VTUViewer.Arrow.AUTO + "\",\""
            + VTUViewer.Arrow.SCALED + "\"]") final String sFieldScaleType,
            @ParamGroupInfo(group = "Filters")
            @ParamInfo(name = "Arrow Scale Factor",
            style = "default",
            options = "invokeOnChange=true; value=0.05",
            nullIsValid = true) final double fieldScaleFactor) {

        if (plotSetup == null) {
            plotSetup = new PlotSetup();
        }
        plotSetup.title = title;
        //
        plotSetup.sRange = sRange;
        plotSetup.minValueRange = minValueRange;
        plotSetup.maxValueRange = maxValueRange;
        //
        plotSetup.bShowLegend = bShowLegend;
        plotSetup.bShowOutline = bShowOutline;
        plotSetup.showOrientation = showOrientation;
        //
        plotSetup.sDataStyle = sDataStyle;
        plotSetup.sDisplayStyle = sDisplayStyle;
        //
        plotSetup.wait = wait;
        //
        plotSetup.warpFactor = warpFactor;
        plotSetup.numContours = numContours;
        plotSetup.arrowStyle = sFieldScaleType;
        plotSetup.fieldScaleFactor = fieldScaleFactor;
        //
        plotSetup.elementInFile = elementInFile;

    }

    static private Visualization createVisualization(File file, PlotSetup plotSetup) {

        // create new Visualization
        final Visualization visualization = new Visualization();

        // set orientation triad
        visualization.setOrientationVisible(plotSetup.showOrientation);

        // set title
        if (!plotSetup.title.isEmpty()) {
            visualization.setTitle(plotSetup.title);
        }

        // get filename
        final String fileName = file.getAbsolutePath();//getAbsoluteFile();

        // read data from file
        vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();
        reader.SetFileName(fileName);
        reader.Update();

        // get grid
        vtkUnstructuredGrid ug = reader.GetOutput();

        // set data field
        if (updateDataArrays(ug, plotSetup) == false) {
            return null;
        }

        // create Data lookup table
        vtkLookupTable defaultLookupTable = createLookupTable(plotSetup.sRange,
                plotSetup.minValueRange, plotSetup.maxValueRange,
                plotSetup.bShowLegend, visualization, plotSetup.dataArray);

        // create Filters
        if (plotSetup.sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

            double factor = plotSetup.fieldScaleFactor;
            if (plotSetup.arrowStyle.equals(Arrow.AUTO)) {
                double[] valueMinMax = defaultLookupTable.GetTableRange();
                double diag = ug.GetLength();
                double max = Math.max(Math.abs(valueMinMax[1]), Math.abs(valueMinMax[0]));
                // this is heuristic, should be improved
                factor = 0.04 * diag / max;
            }

            addVectorFieldFilter(defaultLookupTable, ug,
                    plotSetup.dataType, plotSetup.elementInFile, plotSetup.index, factor,
                    plotSetup.sDisplayStyle, visualization);
        } else {

            if (plotSetup.sDataStyle.equals(DataStyle.NONE)) {

                addPlainDataVisualization(defaultLookupTable, ug,
                        plotSetup.sDisplayStyle, visualization);
            }

            if (plotSetup.sDataStyle.equals(DataStyle.WARP_AUTO)
                    || plotSetup.sDataStyle.equals(DataStyle.WARP_FACTOR)) {

                addWarpDataVisualization(ug, plotSetup.sDataStyle, plotSetup.warpFactor,
                        defaultLookupTable, plotSetup.sDisplayStyle, visualization, plotSetup.dataArray);
            }

            if (plotSetup.sDataStyle.equals(DataStyle.CONTOUR)) {

                addContourFilter(defaultLookupTable, ug,
                        plotSetup.numContours, plotSetup.sDisplayStyle, visualization);
            }
        }

        // add outline filter
        if (plotSetup.bShowOutline) {
            addOutlineFilter(ug, visualization);
        }

        // return created Visualization
        return visualization;
    }

    static private vtkLookupTable createLookupTable(String sRange, double minValueRange, double maxValueRange,
            boolean bShowLegend, final Visualization visualization, vtkDataArray dataArray) {

        vtkLookupTable defaultLookupTable = new vtkLookupTable();

        double[] valRange;

        if (sRange.equals(Range.AUTO)) {

            valRange = getRange(dataArray);
            minValueRange = valRange[0];
            maxValueRange = valRange[1];
        }

        defaultLookupTable.SetTableRange(minValueRange, maxValueRange);

        // something strange
//        defaultLookupTable.SetHueRange(0.0, 1);
//        defaultLookupTable.SetSaturationRange(0.6, 1);
//        defaultLookupTable.SetValueRange(1, 1);

        // blue to red
        defaultLookupTable.SetHueRange(0.66667, 0.0);
        defaultLookupTable.SetSaturationRange(1, 1);
        defaultLookupTable.SetValueRange(1, 1);
        defaultLookupTable.SetAlphaRange(1, 1);
        defaultLookupTable.SetNumberOfColors(256);

        defaultLookupTable.Build();

        if (bShowLegend) {
            visualization.setLookupTable(defaultLookupTable);
        }
        return defaultLookupTable;
    }

    static private void addPlainDataVisualization(
            vtkLookupTable defaultLookupTable, vtkUnstructuredGrid ug,
            String sDisplayStyle, final Visualization visualization) {

        vtkLookupTable plainLookupTable = new vtkLookupTable();
        plainLookupTable.DeepCopy(defaultLookupTable);

        vtkDataSetMapper plainMapper = new vtkDataSetMapper();
        plainMapper.SetInput(ug);

        setSettingsForMapper(sDisplayStyle, plainMapper, plainLookupTable);

        vtkActor plainActor = new vtkActor();
        plainActor.SetMapper(plainMapper);

        setDisplayStyle(plainActor, sDisplayStyle);

        visualization.addActor(plainActor);

    }

    static private void addWarpDataVisualization(vtkUnstructuredGrid ug,
            String sDataStyle, double warpFactor,
            vtkLookupTable defaultLookupTable, String sDisplayStyle,
            final Visualization visualization, vtkDataArray dataArray) {

        double[] valueMinMax = getRange(dataArray);

        double factor = 1.0 / (valueMinMax[1] - valueMinMax[0]);
        if(valueMinMax[1] == valueMinMax[0]){
            if(valueMinMax[1] != 0.0){
                factor = 1.0 / valueMinMax[1];
            } else {
                factor = 1.0;
            }
        }

        if (sDataStyle.equals(DataStyle.WARP_FACTOR)) {
            factor = warpFactor;
        }

        vtkWarpScalar warpScalar = new vtkWarpScalar();
        warpScalar.SetInput(ug);
        warpScalar.SetScaleFactor(factor);
        warpScalar.Update();

        vtkLookupTable warpTable = new vtkLookupTable();
        warpTable.DeepCopy(defaultLookupTable);

        vtkDataSetMapper warpMapper = new vtkDataSetMapper();
        warpMapper.SetInputConnection(warpScalar.GetOutputPort());

        setSettingsForMapper(sDisplayStyle, warpMapper, warpTable);

        vtkActor warpActor = new vtkActor();
        warpActor.SetMapper(warpMapper);

        setDisplayStyle(warpActor, sDisplayStyle);

        visualization.addActor(warpActor);
    }

    static private void addContourFilter(vtkLookupTable defaultLookupTable,
            vtkUnstructuredGrid ug, int numContours, String sDisplayStyle,
            final Visualization visualization) {

        vtkLookupTable contourTable = new vtkLookupTable();
        contourTable.DeepCopy(defaultLookupTable);

        vtkContourFilter contours = new vtkContourFilter();
        contours.SetInput(ug);
        contours.GenerateValues(numContours, contourTable.GetTableRange());

        vtkPolyDataMapper contourMapper = new vtkPolyDataMapper();
        contourMapper.SetInput(contours.GetOutput());

        setSettingsForMapper(sDisplayStyle, contourMapper, contourTable);

        vtkActor contourActor = new vtkActor();
        contourActor.SetMapper(contourMapper);

        setDisplayStyle(contourActor, sDisplayStyle);

        visualization.addActor(contourActor);
    }

    static private void addVectorFieldFilter(vtkLookupTable defaultLookupTable,
            vtkUnstructuredGrid ug, VTUViewer.DataType dataType,
            String elementInFile, int index, double scaleFactor,
            String sDisplayStyle, final Visualization visualization) {

        vtkLookupTable vectorfieldTable = new vtkLookupTable();
        vectorfieldTable.DeepCopy(defaultLookupTable);

        vtkGlyph3D vectorGlyph = new vtkGlyph3D();
        vtkArrowSource arrowSource = new vtkArrowSource();


        if (dataType.equals(DataType.POINT)) {

            ug.GetPointData().SetActiveVectors(elementInFile);

            vectorGlyph.SetSourceConnection(arrowSource.GetOutputPort());
            vectorGlyph.SetInput(ug);
        }

        if (dataType.equals(DataType.CELL)) {
            ug.GetCellData().SetActiveVectors(elementInFile);

            vtkCellCenters cellCentersFilter = new vtkCellCenters();
            cellCentersFilter.SetInputConnection(ug.GetProducerPort());
            cellCentersFilter.SetInput(ug);
            cellCentersFilter.VertexCellsOn();
            cellCentersFilter.Update();

            vectorGlyph.SetSourceConnection(arrowSource.GetOutputPort());
            vectorGlyph.SetInput(cellCentersFilter.GetOutput());
        }

        vectorGlyph.SetScaleModeToScaleByVector();
        vectorGlyph.SetVectorModeToUseVector();
        vectorGlyph.ScalingOn();
        vectorGlyph.OrientOn();
        vectorGlyph.SetColorModeToColorByVector();//color the glyphs
        vectorGlyph.SetScaleFactor(scaleFactor);
        vectorGlyph.Update();


        vtkPolyDataMapper vectorGlyphMapper = new vtkPolyDataMapper();
        vectorGlyphMapper.SetInput(vectorGlyph.GetOutput());

        setSettingsForMapper(sDisplayStyle, vectorGlyphMapper, vectorfieldTable);

        //optional setttings for lightning
        vtkProperty sliceProp = new vtkProperty();
        sliceProp.SetDiffuse(0.0);
        sliceProp.SetSpecular(0.0);
        sliceProp.SetAmbient(1.0);

        vtkActor vectorActor = new vtkActor();
        vectorActor.SetMapper(vectorGlyphMapper);
        vectorActor.SetProperty(sliceProp);

        visualization.addActor(vectorActor);
    }

    static private void addOutlineFilter(vtkUnstructuredGrid ug,
            final Visualization visualization) {

        vtkOutlineFilter outline = new vtkOutlineFilter();
        outline.SetInput(ug);

        vtkPolyDataMapper outlineMapper = new vtkPolyDataMapper();
        outlineMapper.SetInput(outline.GetOutput());

        vtkActor outlineActor = new vtkActor();
        outlineActor.SetMapper(outlineMapper);

        visualization.addActor(outlineActor);
    }

    static private boolean updateDataArrays(vtkUnstructuredGrid ug,
            PlotSetup plotSetup) {

        // try point data
        plotSetup.dataArray = ug.GetPointData().GetArray(plotSetup.elementInFile);
        plotSetup.dataType = DataType.POINT;

        // try cell data
        if (plotSetup.dataArray == null) {
            plotSetup.dataArray = ug.GetCellData().GetArray(plotSetup.elementInFile);
            plotSetup.dataType = DataType.CELL;
        }

        // if not successful, return false
        if (plotSetup.dataArray == null) {
            plotSetup.dataType = DataType.INVALID;
            return false;
        }

        if (plotSetup.sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {
            switch (plotSetup.dataType) {
                case POINT:
                    ug.GetPointData().SetVectors(plotSetup.dataArray);
                    break;
                case CELL:
                    ug.GetCellData().SetVectors(plotSetup.dataArray);
                    break;
                default:
                    throw new RuntimeException("Data type not found.");
            }
        } else {
            switch (plotSetup.dataType) {
                case POINT:
                    ug.GetPointData().SetScalars(plotSetup.dataArray);
                    break;
                case CELL:
                    ug.GetCellData().SetScalars(plotSetup.dataArray);
                    break;
                default:
                    throw new RuntimeException("Data type not found.");
            }
        }

        return true;
    }

    static private double[] getRange(vtkDataArray dataArray) {

        double[] valueMinMax;

        if (dataArray != null) {
            valueMinMax = dataArray.GetRange();
        } else {
            VMessage.exception("ERROR", "The choosen parameter combination led to"
                    + " an invalid state. Please check your selection. \n"
                    + " E.g: select only vector field as DisplayStyle if the a"
                    + " vector field is selected from a vtu file.");
            valueMinMax = null;
        }

        return valueMinMax;
    }

    static private void setSettingsForMapper(String sDisplayStyle,
            vtkMapper mapper, vtkLookupTable lookupTable) {

        mapper.SetLookupTable(lookupTable);

        if (!sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

            mapper.SetScalarRange(lookupTable.GetTableRange());
            mapper.SetColorModeToMapScalars();
            mapper.ScalarVisibilityOn();
        } else {

            mapper.SetScalarRange(lookupTable.GetTableRange());
            mapper.ScalarVisibilityOn();//color for glyphs
//            mapper.ScalarVisibilityOff();
        }

        mapper.Update();
    }

    static private void setDisplayStyle(vtkActor actor, String style) {

        if (style.equals(DisplayStyle.SURFACE)) {
            actor.GetProperty().SetRepresentationToSurface();
        } else if (style.equals(DisplayStyle.SURFACE_EDGE)) {
            actor.GetProperty().SetRepresentationToSurface();
            actor.GetProperty().EdgeVisibilityOn();
        } else if (style.equals(DisplayStyle.WIREFRAME)) {
            actor.GetProperty().SetRepresentationToWireframe();
            actor.GetProperty().SetAmbient(1.0);
            actor.GetProperty().SetDiffuse(0.0);
            actor.GetProperty().SetSpecular(0.0);
        } else if (style.equals(DisplayStyle.POINTS)) {
            actor.GetProperty().SetRepresentationToPoints();
            actor.GetProperty().SetPointSize(2.5);
        } else if (style.equals(DisplayStyle.VECTORFIELD)) {
//            actor.GetProperty().SetRepresentationToPoints();
//            actor.ApplyProperties();                    
        } else {
            throw new RuntimeException("Style not found.");
        }
    }

    static private ArrayList<File> getAllFilesInFolder(@ParamInfo(style = "load-folder-dialog") File dir,
            @ParamInfo(name = "beginning, e.g. \"file00\"")
            final String startsWith,
            @ParamInfo(name = "ending, e.g. \"vtu\"")
            final String ending) {

        ArrayList<File> result = new ArrayList<File>();

        if (dir != null && dir.isDirectory()) {
            for (File f : dir.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathName) {
                    boolean fileAccept = pathName.getName().toLowerCase().endsWith("." + ending) || pathName.isDirectory();

                    int dot = pathName.getPath().lastIndexOf(".");
                    int sep = pathName.getPath().lastIndexOf(File.separator);

                    String fileName = pathName.getPath().substring(sep + 1, dot);

                    boolean nameAccept = startsWith.equals("") || fileName.startsWith(startsWith + "_");

                    return fileAccept && nameAccept;
                }
            }))// fore end 
            {
                if (f.isFile()) {
                    result.add(f);
                }
            }

        } // if a file is directly choosen, add it
        else if (dir != null && dir.isFile()) {
            result.add(dir);
        }

        return result;
    }

    @MethodInfo(noGUI = true, callOptions = "assign-window")
    public void setParameterVisibility(VisualObject vObj) {

        final DefaultMethodRepresentation mVisualizeRep = vObj.getObjectRepresentation().
                getMethodBySignature("visualize", MethodRequest.class,
                File.class, String.class,
                boolean.class);

        final DefaultMethodRepresentation mSetupRep = vObj.getObjectRepresentation().
                getMethodBySignature("setup", MethodRequest.class,
                String.class, long.class, String.class,
                String.class, double.class, double.class,
                boolean.class, boolean.class, boolean.class,
                String.class, String.class, double.class, int.class, String.class, double.class);
        final ActionListener fileOrFolderListener = new ActionListener() {
            // set visibility for startsWith (2)
            // depending on value of parameter folder (1)

            public void actionPerformed(ActionEvent e) {

                if (e.getActionCommand().equals(
                        LoadObserveFileType.FILE_OR_FOLDER_LOADED_ACTION)) {

                    File fileOrFolder = (File) mVisualizeRep.getParameter(1).getViewValueWithoutValidation();

                    // if folder does not exist, do nothing
                    if (fileOrFolder == null) {
                        return;
                    }

                    TypeRepresentationBase tRep = mVisualizeRep.getParameter(2);

                    if (fileOrFolder.isDirectory()) {

                        tRep.setVisible(true);
                        tRep.getConnector().setVisible(true);
                        mVisualizeRep.getParameter(1).setValueName("Folder");

                    } else if (fileOrFolder.isFile()) {

                        tRep.setVisible(false);
                        tRep.getConnector().setVisible(false);
                        mVisualizeRep.getParameter(1).setValueName("File");
                    }

                }
            }
        };
        final ActionListener sRangeListener = new ActionListener() {
            // set visibility for min- max-ValueRange (8 & 9)
            // depending on value of parameter sRange (7)

            public void actionPerformed(ActionEvent e) {

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sRange = (String) mSetupRep.getParameter(4).getViewValueWithoutValidation();

                    TypeRepresentationBase tRep8 = mSetupRep.getParameter(5);
                    TypeRepresentationBase tRep9 = mSetupRep.getParameter(6);

                    if (sRange.equals(Range.AUTO)) {

                        tRep8.setVisible(false);
                        tRep8.getConnector().setVisible(false);

                        tRep9.setVisible(false);
                        tRep9.getConnector().setVisible(false);

                    } else if (sRange.equals(Range.MIN_MAX)) {

                        tRep8.setVisible(true);
                        tRep8.getConnector().setVisible(true);

                        tRep9.setVisible(true);
                        tRep9.getConnector().setVisible(true);
                    }
                }
            }
        };
        final ActionListener warpListener = new ActionListener() {
            // set visibility for warpfactor (15)
            // depending on value of parameter sDataStyle (14)

            public void actionPerformed(ActionEvent e) {

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sDataStyle = (String) mSetupRep.getParameter(11).getViewValueWithoutValidation();

                    TypeRepresentationBase tRep = mSetupRep.getParameter(12);

                    if (sDataStyle.equals(DataStyle.WARP_FACTOR)) {

                        tRep.setVisible(true);
                        tRep.getConnector().setVisible(true);

                    } else {

                        tRep.setVisible(false);
                        tRep.getConnector().setVisible(false);
                    }

                }
            }
        };
        final ActionListener contourListener = new ActionListener() {
            // set visibility for contourfactor (16)
            // depending on value of parameter sDataStyle (14)

            public void actionPerformed(ActionEvent e) {

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sDataStyle = (String) mSetupRep.getParameter(11).getViewValueWithoutValidation();

                    TypeRepresentationBase tRep = mSetupRep.getParameter(13);

                    if (sDataStyle.equals(DataStyle.CONTOUR)) {
                        tRep.setVisible(true);
                        tRep.getConnector().setVisible(true);

                    } else {
                        tRep.setVisible(false);
                        tRep.getConnector().setVisible(false);
                    }
                }
            }
        };

        final ActionListener fieldScaleListener = new ActionListener() {
            // set visibility for contourfactor (17)
            // depending on value of parameter sDisplayStyle (13)

            public void actionPerformed(ActionEvent e) {

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sDisplayStyle = (String) mSetupRep.getParameter(10).getViewValueWithoutValidation();

                    TypeRepresentationBase tRep14 = mSetupRep.getParameter(14);
                    TypeRepresentationBase tRep15 = mSetupRep.getParameter(15);

                    if (sDisplayStyle.equals(DisplayStyle.VECTORFIELD)) {

                        tRep14.setVisible(true);
                        tRep14.getConnector().setVisible(true);
                        
                        String sDisplayStyle2 = (String) mSetupRep.getParameter(14).getViewValueWithoutValidation();
                        TypeRepresentationBase tRep = mSetupRep.getParameter(15);

                        if (sDisplayStyle2.equals(Arrow.SCALED)) {

                            tRep15.setVisible(true);
                            tRep15.getConnector().setVisible(true);

                        } else {

                            tRep15.setVisible(false);
                            tRep15.getConnector().setVisible(false);
                        }

                    } else {

                        tRep14.setVisible(false);
                        tRep14.getConnector().setVisible(false);
                        tRep15.setVisible(false);
                        tRep15.getConnector().setVisible(false);

                    }
                }
            }
        };

        final ActionListener fieldTypeListener = new ActionListener() {
            // set visibility for contourfactor (17)
            // depending on value of parameter sDisplayStyle (13)

            public void actionPerformed(ActionEvent e) {

                if (e.getActionCommand().equals(
                        //                        TypeRepresentationBase.SET_VIEW_VALUE_ACTION
                        SelectionInputType.SELECTION_CHANGED_ACTION)) {

                    String sDisplayStyle = (String) mSetupRep.getParameter(14).getViewValueWithoutValidation();

                    TypeRepresentationBase tRep = mSetupRep.getParameter(15);

                    if (sDisplayStyle.equals(Arrow.SCALED)) {

                        tRep.setVisible(true);
                        tRep.getConnector().setVisible(true);

                    } else {

                        tRep.setVisible(false);
                        tRep.getConnector().setVisible(false);

                    }
                }
            }
        };

        mSetupRep.getParameter(
                14).getActionListeners().add(fieldTypeListener);
        mSetupRep.getParameter(
                10).getActionListeners().add(fieldScaleListener);
        mSetupRep.getParameter(
                11).getActionListeners().add(contourListener);
        mSetupRep.getParameter(
                11).getActionListeners().add(warpListener);
        mSetupRep.getParameter(
                4).getActionListeners().add(sRangeListener);

        mVisualizeRep.getParameter(
                1).getActionListeners().add(fileOrFolderListener);


        // START init action (dirty but needed)
        ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

        listeners.add(fieldScaleListener);

        listeners.add(fieldTypeListener);

        listeners.add(contourListener);

        listeners.add(warpListener);

        listeners.add(sRangeListener);

        listeners.add(fileOrFolderListener);

        for (ActionListener al : listeners) {
            al.actionPerformed(new ActionEvent(this, 0, TypeRepresentationBase.SET_VIEW_VALUE_ACTION));
            al.actionPerformed(new ActionEvent(this, 0, LoadObserveFileType.FILE_OR_FOLDER_LOADED_ACTION));
            al.actionPerformed(new ActionEvent(this, 0, SelectionInputType.SELECTION_CHANGED_ACTION));
        }
        // END init action (dirty but needed)
        // END setting some parameters visable or not depending on others
    }
}
