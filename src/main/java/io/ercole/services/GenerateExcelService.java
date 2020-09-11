// Copyright (c) 2019 Sorint.lab S.p.A.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package io.ercole.services;

import io.ercole.model.CurrentHost;
import io.ercole.repositories.CurrentHostRepository;
import io.ercole.utilities.JsonFilter;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * The type Generate excel service.
 */
@Service
public class GenerateExcelService {

    private static String cpuM = "CPUModel";
    private static String vrs = "Version";

    @Autowired
    private CurrentHostRepository currentRepo;


    /**
     * Init excel response entity.
     *
     * @return the response entity
     * @throws IOException      the io exception
     * @throws RuntimeException the runtime exception
     */
    public ResponseEntity<byte[]> initExcel() throws IOException {
        Iterable<CurrentHost> iterable = currentRepo.findAllByOrderByHostnameAsc();


        try (Workbook workbook = new XSSFWorkbook(new ClassPathResource("template_hosts.xlsm").getInputStream())) {

            XSSFSheet xssfSheet = ((XSSFWorkbook) workbook).getSheet("Database_&_EBS_DB_Tier");
            //number of row where we will write (row 0,1,2 contains the heading of the table)
            int rowNumber = 3;

            for (CurrentHost host : iterable) {
                //get json HostInfo (it contains another 16 field)
                JSONObject root = new JSONObject(host.getHostInfo());
                //get json ExtraInfo (it contains another 2 json array - databases and features)
                JSONObject root2 = new JSONObject(host.getExtraInfo());
                //get json databases
                JSONArray arrayExtraInfo = root2.getJSONArray("Databases");
                //In the db there are some hosts that don't serve databases
                if (arrayExtraInfo.length() == 0) {
                    continue;
                }

                
                for (int j = 0; j < arrayExtraInfo.length(); j++) {
                        XSSFRow row = xssfSheet.createRow(rowNumber);

                        String[] dataOfHost = new String[40];

                        JSONObject database = arrayExtraInfo.getJSONObject(j);
                        //get json array features (after we will create jsonObject features from this array -getFeatures())
                        JSONArray features = database.getJSONArray("Features");
                        JSONArray licenses = database.getJSONArray("Licenses");

                        //index 
                        int indexChiocciola = root.getString(cpuM).lastIndexOf(' ');
                        int indexVersion = database.getString(vrs).indexOf(' ');

                        //save data into array
                        if (root.getString("Type").equals("VMWARE") || root.getString("Type").equals("OVM")) {
                                dataOfHost[0]  =
                                        host.getAssociatedClusterName();                     //physical server name
                                dataOfHost[1]  =
                                        host.getHostname();        //virtual server name
                        } else {
                                dataOfHost[0]  =
                                        host.getHostname();                     //physical server name
                                dataOfHost[1]  =
                                        host.getAssociatedClusterName();        //virtual server name
                        }
                        switch (root.getString("Type")) {
                                case "PH":
                                        dataOfHost[2]  = "";
                                        break;
                                case "OVM":
                                        if (String.valueOf(root.get(cpuM)).contains("SPARC")) {
                                                dataOfHost[2]  = "OVM Server for SPARC";
                                        } else {
                                                dataOfHost[2] = "OVM Server for x86";
                                        }
                                        break;
                                case "VMWARE":
                                        dataOfHost[2]  = "VMware";
                                        break;
                                case "HYPERV":
                                        dataOfHost[2]  = "Hyper-V";
                                        break;
                                default:
                                        dataOfHost[2] = root.getString("Type");
                        }

                        System.out.println(database.getString("Name"));
                        dataOfHost[3]  = database.getString("Name");
                        dataOfHost[5]  = host.getEnvironment();
                        dataOfHost[6]  = JsonFilter.getTrueFeatures(features);
                        dataOfHost[7]  = JsonFilter.getManagementPack(features);
                        if (String.valueOf(database.get(vrs)).contains(" ")) {
                                dataOfHost[12] =         String.valueOf(database.get(vrs)).split(" ")[0]; //product version
                                dataOfHost[13] = String.valueOf(database.get(vrs)).substring(indexVersion);
                        } else {
                                dataOfHost[12] =         String.valueOf(database.get(vrs)); //product version
                                dataOfHost[13] = String.valueOf(database.get(vrs));

                        }
                        if (String.valueOf(database.get(vrs)).toLowerCase().contains("standard")) {
                                dataOfHost[13] = "SE";
                        } else if (String.valueOf(database.get(vrs)).toLowerCase().contains("enterprise")) {
                                dataOfHost[13] = "EE";
                        }
                        dataOfHost[14] = "processor";
                        
                        dataOfHost[15] = "???";
                        for (int k = 0; k < licenses.length(); k++) {
                                JSONObject lic = licenses.getJSONObject(k);
                                if (lic.getFloat("Count") > 0 && (lic.getString("Name").equals("Oracle EXE") || lic.getString("Name").equals("Oracle STD") || lic.getString("Name").equals("Oracle ENT"))) {
                                        dataOfHost[15] = Float.toString(lic.getFloat("Count"));                                  
                                }
                        }
                        dataOfHost[27] = String.valueOf(root.get(cpuM));             //processor model
                        dataOfHost[28] = String.valueOf(root.get("Socket")); // processor socket
                        int coresPerProcessor = root.getInt("CPUCores"); // core per processor
                        if (root.getInt("CPUCores") >= root.getInt("Socket") && root.getInt("Socket") != 0) {
                                coresPerProcessor = root.getInt("CPUCores") / root.getInt("Socket"); // core per processor
                        }
                        dataOfHost[29] = String.valueOf(coresPerProcessor);
                        int physicalCores;
                        if (root.getInt("Socket") == 0) {
                                physicalCores =  coresPerProcessor;
                        } else {
                                physicalCores = coresPerProcessor * root.getInt("Socket");
                        }
                        dataOfHost[30] = Integer.toString(physicalCores); 
                        if (String.valueOf(root.get(cpuM)).contains("SPARC")) {
                                dataOfHost[31] = "8";
                        } else {
                                dataOfHost[31] = "2";
                        }
                        
                        dataOfHost[32] =
                                String.valueOf(root.get(cpuM)).substring(indexChiocciola); //processor speed

                        dataOfHost[34] =
                                String.valueOf(root.get("OS"));  //operating system


                        //insert data of host into a new cell
                        for (int i = 0; i < 40; i++) {
                                Cell cell = row.createCell(i + 1);
                                cell.setCellValue(dataOfHost[i]);
                        }
                        rowNumber++;
                }
            }
            //writing changes in the open file (templateVuoto)

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                workbook.write(outputStream);
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Hosts.xlsm");
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                        .body(outputStream.toByteArray());
            }
        }
    }


    /**
     * Init excel response entity.
     *
     * @return the response entity
     * @throws IOException      the io exception
     * @throws RuntimeException the runtime exception
     */
    public ResponseEntity<byte[]> initExcelWithoutTemplate() throws IOException {
        Iterable<CurrentHost> iterable = currentRepo.findAllByOrderByHostnameAsc();


        try (Workbook workbook = new XSSFWorkbook()) {

            XSSFSheet xssfSheet = ((XSSFWorkbook) workbook).createSheet();
            //header row
            XSSFRow rowHeader = xssfSheet.createRow(0);
            rowHeader.createCell(0).setCellValue("hostname");
            rowHeader.createCell(1).setCellValue("env");
            rowHeader.createCell(2).setCellValue("host type");
            rowHeader.createCell(3).setCellValue("cluster");
            rowHeader.createCell(4).setCellValue("physical host");
            rowHeader.createCell(5).setCellValue("last update");
            rowHeader.createCell(6).setCellValue("databases");
            rowHeader.createCell(7).setCellValue("OS");
            rowHeader.createCell(8).setCellValue("kernel");
            rowHeader.createCell(9).setCellValue("oracle cluster");
            rowHeader.createCell(10).setCellValue("sun cluster");
            rowHeader.createCell(11).setCellValue("veritas cluster");
            rowHeader.createCell(12).setCellValue("virtual");
            rowHeader.createCell(13).setCellValue("host type");
            rowHeader.createCell(14).setCellValue("cpu threads");
            rowHeader.createCell(15).setCellValue("cpu cores");
            rowHeader.createCell(16).setCellValue("sockets");
            rowHeader.createCell(17).setCellValue("mem total");
            rowHeader.createCell(18).setCellValue("swap total");

            //data rows
            int rowNumber = 1;
            for (CurrentHost host : iterable) {
                XSSFRow row = xssfSheet.createRow(rowNumber);
                //get json HostInfo (it contains another 16 field)
                JSONObject root = new JSONObject(host.getHostInfo());
                // //get json ExtraInfo (it contains another 2 json array - databases and features)
                // JSONObject root2 = new JSONObject(host.getExtraInfo());
                // //get json databases
                // JSONArray arrayExtraInfo = root2.getJSONArray("Databases");
                //In the db there are some hosts that don't serve databases
                // if (arrayExtraInfo.length() == 0) {
                //     continue;
                // }
                // JSONObject database = arrayExtraInfo.getJSONObject(0);
                // //get json array features (after we will create jsonObject features from this array -getFeatures())
                // JSONArray features = database.getJSONArray("Features");

                //save data into array
                String[] dataOfHost = new String[20];
                dataOfHost[0]  = host.getHostname();                     
                dataOfHost[1]  = host.getEnvironment();
                dataOfHost[2]  = host.getHostType();
                dataOfHost[3]  = host.getAssociatedClusterName();                   
                dataOfHost[4]  = host.getAssociatedHypervisorHostname();
                dataOfHost[5]  = host.getUpdated().toString();
                dataOfHost[6]  = host.getDatabases();
                dataOfHost[7] =  root.getString("OS");
                dataOfHost[8]  = root.getString("Kernel");
                dataOfHost[9]  = "" + root.getBoolean("OracleCluster");
                dataOfHost[10]  = "" + root.getBoolean("SunCluster"); 
                dataOfHost[11] = "" + root.getBoolean("VeritasCluster");
                dataOfHost[12] = "" + root.getBoolean("Virtual");
                dataOfHost[13] = root.getString("Type"); 
                dataOfHost[14] = "" + root.getInt("CPUThreads");
                dataOfHost[15] = "" + root.getInt("CPUCores");
                dataOfHost[16] = "" + root.getInt("Socket"); 
                dataOfHost[17] = "" + root.getInt("MemoryTotal"); 
                dataOfHost[18] = "" + root.getInt("SwapTotal"); 
                dataOfHost[19] = root.getString("CPUModel");

                //insert data of host into a new cell
                for (int i = 0; i < dataOfHost.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(dataOfHost[i]);
                }
                rowNumber++;
            }
            //writing changes in the open file (templateVuoto)

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                workbook.write(outputStream);
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=HostsRaw.xlsx");
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                        .body(outputStream.toByteArray());
            }
        }
    }
}
