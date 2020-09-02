
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main
{
    static List<FileTypeOccurrence> fileTypeOccurrenceList = new ArrayList<>();
    static List<File> emptyDirectories = new ArrayList<>();
    static List<File> seasonDirectoryOutliers = new ArrayList<>();

    static List<String> deleteTypes = Arrays.asList("txt", "srt", "website", "jpg", "png", "nfo");
    static String maindirpath = "Z:\\Plex Media\\Shows";

    public static void main(String[] args) throws IOException
    {
        File maindir = new File(maindirpath);
        if(maindir.exists() && maindir.isDirectory())
        {
            RecursiveSearch(maindir.listFiles(),0,0);
            printFindings();
            promptEnterKey();
            deleteUnwatedFiles();
        }
    }

    /**
     * Loops through a specified directory printing the structure and storing file type occurrences
     *
     * @param arr
     * @param index
     * @param level
     */
    static void RecursiveSearch(File[] arr, int index, int level)
    {
        // terminate condition
        if(index == arr.length)
            return;

        File currentFile = arr[index];

        // for files
        if(currentFile.isFile())
        {
            addFileTypeOccurence(currentFile);
        }

        // for sub-directories
        else if(currentFile.isDirectory())
        {
            File[] files = currentFile.listFiles();
            List<File> filesList = Arrays.asList(currentFile.listFiles());
            for (File file : filesList)
            {
                // if inside the seasons directory
                if (file.getName().matches("Season [0-9][0-9]"))
                {
                    for (File innerFile : filesList)
                    {
                        // if the file is not a season folder
                        if (!innerFile.getName().matches("Season [0-9][0-9]"))
                        {
                            seasonDirectoryOutliers.add(innerFile);
                        }
                    }
                    break;
                }
            }

            if (files.length == 0)
                emptyDirectories.add(currentFile);

            // recursion for sub-directories
            if (files != null)
                RecursiveSearch(files, 0, level + 1);
        }

        // recursion for main directory
        RecursiveSearch(arr, ++index, level);
    }

    /**
     * Print out the file findings
     */
    static void printFindings()
    {
        System.out.println("\nFileTypeOccurrence Findings");
        for(FileTypeOccurrence fileTypeOccurrence : fileTypeOccurrenceList)
        {
            if (fileTypeOccurrence.getDelete())
            {
                System.out.println("\t" + fileTypeOccurrence.toStringWithFiles());
            }
        }
        for(FileTypeOccurrence fileTypeOccurrence : fileTypeOccurrenceList)
        {
            // only print files if they're marked for deletion
            if (!fileTypeOccurrence.getDelete())
            {
                System.out.println("\t" + fileTypeOccurrence.toString());
            }
        }
        System.out.println("\nSeason Directory Outliers");
        for(File file : seasonDirectoryOutliers)
        {
            System.out.println("\t" + file.getPath());
        }
        System.out.println("\nEmpty Directories");
        for(File file : emptyDirectories)
        {
            System.out.println("\t" + file.getPath());
        }
    }

    /**
     * Require pressing enter in the console before continuing with execution
     */
    static void promptEnterKey() {
        System.out.println("\nPress \"ENTER\" to delete Empty Directories, Season Directory Outliers and files of type, " + deleteTypes.toString());
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    /**
     * Add the {{@link File}} to the {{@link FileTypeOccurrence}} list
     *
     * @param file
     */
    static void addFileTypeOccurence(File file)
    {
        String fileName = file.getName();
        String fileExt = getExtension(fileName);
        boolean add = true;

        // handle storing file type occurrences
        for (FileTypeOccurrence fileTypeOccurrence : fileTypeOccurrenceList)
        {
            if (fileTypeOccurrence.fileType.equals(fileExt))
            {
                fileTypeOccurrence.update(file);
                add = false;
                break;
            }
        }
        if (add)
        {
            fileTypeOccurrenceList.add(new FileTypeOccurrence(file, fileExt));
        }
    }

    /**
     * Return fileType string
     * @param filename the name of the file with fileType
     * @return the string fileType of the file
     */
    static String getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1)).get().toLowerCase();
    }

    /**
     * Delete all the unwanted files stored in lists above
     *
     * @throws IOException
     */
    static void deleteUnwatedFiles() throws IOException
    {
        // delete all files with the specific extension
        System.out.println("\nDeleting Unwanted File Types");
        for(FileTypeOccurrence fileTypeOccurrence : fileTypeOccurrenceList)
        {
            if (fileTypeOccurrence.getDelete())
            {
                for(File fileToDelete : fileTypeOccurrence.getFiles())
                {
                    System.out.println("\tDeleting - " + fileToDelete.getPath());
                    deleteFile(fileToDelete);
                }
            }
        }
        // delete all season directory outliers
        System.out.println("\nDeleting Season Directory Outliers");
        for(File file : seasonDirectoryOutliers)
        {
            System.out.println("\tDeleting - " + file.getPath());
            deleteFile(file);
        }
        // delete all empty directories
        System.out.println("\nDeleting Empty Directories");
        for(File file : emptyDirectories)
        {
            System.out.println("\tDeleting - " + file.getPath());
            deleteFile(file);
        }
    }

    /**
     * Handle deleting a file whether it's a file or directory
     *
     * @param garbFile
     * @throws IOException
     */
    static void deleteFile(File garbFile) throws IOException
    {
        if (garbFile.exists())
        {
            if (garbFile.isFile())
            {
                garbFile.delete();
            }
            else
            {
                Files.walk(garbFile.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    /**
     * Class to count occurrence of extensions and whether to delete them
     */
    static class FileTypeOccurrence
    {
        List<File> files = new ArrayList<>();
        String fileType;
        int occurence;
        boolean delete;

        public FileTypeOccurrence(File file, String fileType)
        {
            files.add(file);
            this.fileType = fileType;
            this.occurence = 1;
            this.delete = setDelete(fileType);
        }

        public void update(File file)
        {
            files.add(file);
            this.occurence += 1;
        }

        boolean setDelete(String ext)
        {
            return deleteTypes.contains(ext) || ext.equals("folder");
        }

        public boolean getDelete()
        {
            return delete;
        }

        public String getFileType()
        {
            return fileType;
        }

        public List<File> getFiles()
        {
            return files;
        }

        @Override
        public String toString()
        {
            return "FileTypeOccurrence{" +
                    "fileType='" + fileType + '\'' +
                    ", occurence=" + occurence +
                    ", delete=" + delete +
                    '}';
        }

        /**
         * @return toString including the list of files
         */
        public String toStringWithFiles()
        {
            return "FileTypeOccurrence{" +
                    "fileType='" + fileType + '\'' +
                    ", occurence=" + occurence +
                    ", delete=" + delete +
                    "\n\t\tfiles="+ filesToString() +
                    '}';
        }

        String filesToString()
        {
            String filesString = "[";
            for (File file : files)
            {
                filesString = filesString.concat("\n\t\t\t" + file.getPath());
            }
            return filesString.concat("\n\t\t]");
        }
    }
}
