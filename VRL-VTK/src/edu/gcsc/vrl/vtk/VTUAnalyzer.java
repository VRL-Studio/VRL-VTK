/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gcsc.vrl.vtk;

import eu.mihosoft.vrl.types.observe.FileAnalyzer;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import vtk.vtkUnstructuredGrid;
import vtk.vtkXMLUnstructuredGridReader;

/**
 *
 * @author Christian Poliwoda <christian.poliwoda@gcsc.uni-frankfurt.de>
 */
class VTUAnalyzer implements FileAnalyzer {

    private String ending = "vtu";
    private String startsWith = "";

    public VTUAnalyzer() {
    }

    /**
     * If file is a dir the first file with the correct ending is choosen, else
     * the file itself.
     *
     * @param file that should be analysed
     *
     * @return the
     */
    public List<String> analyzeFile(File file) {
        //delete previous entries resp. new list
        List<String> fileEntries = new ArrayList<String>();

        ArrayList<File> files = getAllFilesInFolder(file, getStartsWith());

        if (files != null && !files.isEmpty()) {

            File lastFile = files.get(0);

            final String fileName = lastFile.getAbsolutePath();

            vtkXMLUnstructuredGridReader reader = new vtkXMLUnstructuredGridReader();

//            System.out.println("-- reader = "+ reader);
            reader.SetFileName(fileName);
            reader.Update();
            vtkUnstructuredGrid ug = reader.GetOutput();

            ////////////////////////////////////
            // get point Data for component
            ////////////////////////////////////
            int numVisCompData = ug.GetPointData().GetNumberOfArrays();

            System.out.println("ELEMENTS/ARRAYS IN FILE:");
            for (int i = 0; i < numVisCompData; i++) {
                fileEntries.add(ug.GetPointData().GetArrayName(i));
            }
        }

        return fileEntries;
    }

    private ArrayList<File> getAllFilesInFolder(File dir, final String startsWith) {

        ArrayList<File> result = new ArrayList<File>();

        if (dir != null && dir.isDirectory()) {

            System.out.println(getClass().getSimpleName() + " getAllFilesInFolder() DIR");

            for (File f : dir.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathName) {
                    boolean fileAccept = pathName.getName().toLowerCase().endsWith("." + ending) || pathName.isDirectory();

                    int dot = pathName.getPath().lastIndexOf(".");
                    int sep = pathName.getPath().lastIndexOf(File.separator);

                    String fileName = pathName.getPath().substring(sep + 1, dot);

                    boolean nameAccept = startsWith.equals("") || fileName.startsWith(startsWith+"_");

                    return fileAccept && nameAccept;
                }
            })) {
                if (f.isFile()) {
                    result.add(f);
                }
            }

        } else if (dir != null && dir.isFile()) {
            System.out.println(getClass().getSimpleName() + " getAllFilesInFolder() FILE");
            result.add(dir);
        } 

        return result;
    }

    /**
     * @return the startsWith
     */
    public String getStartsWith() {
        return startsWith;
    }

    /**
     * @param startsWith the startsWith to set
     */
    public void setStartsWith(String startsWith) {
        this.startsWith = startsWith;
    }
}
