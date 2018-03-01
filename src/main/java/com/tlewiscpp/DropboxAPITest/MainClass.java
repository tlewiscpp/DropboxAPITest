package com.tlewiscpp.DropboxAPITest;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.Calendar;
import java.util.Scanner;

public class MainClass {
    private static String accessToken;

    private static String getAccessToken(String filePath) {
        try {
            BufferedReader buffer = new BufferedReader(new FileInputStream(filePath));
            String line=buffer.readLine();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String getApplicationName() {
        try {
            return new File(MainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String args[]) throws DbxException {
        if (args.length < 1) {
            System.out.println(MessageFormat.format("Usage: {0} [PathToDropBoxAPIToken]", getApplicationName()));
            System.exit(1);
        }

        accessToken = getAccessToken(args[1]);
        
        // Create Dropbox client
        DbxRequestConfig config = new DbxRequestConfig("dropbox/DropboxAPITest", "en_US");
        DbxClientV2 client = new DbxClientV2(config, accessToken);

        FullAccount account = client.users().getCurrentAccount();
        System.out.println(account.getName().getDisplayName());

        // Get files and folder metadata from Dropbox root directory
        ListFolderResult result = client.files().listFolder("/documents/activitylog/");
        String targetDocumentName = "";
        String targetDocumentPath = "";
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                if (metadata.getName().startsWith(Integer.toString(Year.now().getValue()))) {
                    targetDocumentName = metadata.getName();
                    targetDocumentPath = metadata.getPathLower();
                }
                System.out.println(metadata.getPathDisplay());
            }

            if (!result.getHasMore()) {
                break;
            }

            result = client.files().listFolderContinue(result.getCursor());
        }
        if (targetDocumentName.isEmpty()) {
            System.out.println("Could not find activity log for current year");
            return;
        }
        System.out.println("Found document path " + targetDocumentPath);
        //String tempDocumentName = targetDocumentName + ".tmp";
        String tempDocumentName = targetDocumentName;
        OutputStream targetFile = null;
        try {
            targetFile = new FileOutputStream(tempDocumentName);
            client.files().download(targetDocumentPath).download(targetFile);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }

        printDateReport("02/26/2018");

        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        cal.getTime();
        /*
        try {
            inputStream = new FileInputStream()
            FileMetadata metadata = client.files().uploadBuilder(targetDocument.getPathDisplay()).uploadAndFinish();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
        */
        try {
            targetFile.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
        deleteFile(tempDocumentName);

    }

    private static void deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            boolean deleteResult = file.delete();
            if (!deleteResult) {
                System.out.println("Failed to delete file \"" + filePath + "\"");
            } else {
                System.out.println("Successfully deleted file \"" + filePath + "\"");
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }

    }

    private static void printDateReport(String dateString) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(dateString));
            String workbookFileName = Integer.toString(calendar.get(Calendar.YEAR)) + "-Activity-Log.xlsx";

            Workbook workBook = WorkbookFactory.create(new File(workbookFileName));
            Sheet targetSheet = workBook.getSheet(getMonthForInt(calendar.get(Calendar.MONTH)));
            Row targetRow = targetSheet.getRow(ActivityLogConstants.ROW_OFFSET + calendar.get(Calendar.DAY_OF_MONTH));
            double runDistance = Double.parseDouble(targetRow.getCell(ActivityLogConstants.ColumnIndex.RUN_DISTANCE).getStringCellValue());
            double cycleDistance = Double.parseDouble(targetRow.getCell(ActivityLogConstants.ColumnIndex.CYCLE_DISTANCE).getStringCellValue());
            double swimDistance = Double.parseDouble(targetRow.getCell(ActivityLogConstants.ColumnIndex.SWIM_DISTANCE).getStringCellValue());
            double foodTotals = Double.parseDouble(targetRow.getCell(ActivityLogConstants.ColumnIndex.FOOD_TOTALS).getStringCellValue());
            System.out.print(MessageFormat.format("Totals for {0}", calendar.toString()));
            System.out.print(MessageFormat.format("RunDistance = {0}", runDistance));
            System.out.print(MessageFormat.format("CycleDistance = {0}", cycleDistance));
            System.out.print(MessageFormat.format("SwimDistance = {0}", swimDistance));
            System.out.print(MessageFormat.format("FoodDistance = {0}", foodTotals));
            workBook.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }

    private static String getMonthForInt(int num) {
        String month = "wrong";
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        if (num >= 0 && num <= 11 ) {
            month = months[num];
        }
        return month;
    }

    /*Write document out*//*
    FileOutputStream editedFile = new FileOutputStream(targetDocumentName);
    workBook.write(editedFile);
    editedFile.close();
    */
}
