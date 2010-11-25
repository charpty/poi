/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.ss.usermodel;

import junit.framework.TestCase;
import org.apache.poi.ss.ITestDataProvider;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.Calendar;

/**
 * Common superclass for testing automatic sizing of sheet columns
 *
 * @author Yegor Kozlov
 */
public abstract class BaseTestSheetAutosizeColumn extends TestCase {

    private final ITestDataProvider _testDataProvider;

    protected BaseTestSheetAutosizeColumn(ITestDataProvider testDataProvider) {
        _testDataProvider = testDataProvider;
    }

    // TODO should we have this stuff in the FormulaEvaluator?
    private void evaluateWorkbook(Workbook workbook){
        FormulaEvaluator eval = workbook.getCreationHelper().createFormulaEvaluator();
        for(int i=0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (Row r : sheet) {
                for (Cell c : r) {
                    if (c.getCellType() == Cell.CELL_TYPE_FORMULA){
                        eval.evaluateFormulaCell(c);
                    }
                }
            }
        }
    }

    public void testNumericCells(){
        Workbook workbook = _testDataProvider.createWorkbook();
        DataFormat df = workbook.getCreationHelper().createDataFormat();
        Sheet sheet = workbook.createSheet();

        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue(0); // getCachedFormulaResult() returns 0 for not evaluated formula cells
        row.createCell(1).setCellValue(10);
        row.createCell(2).setCellValue("10");
        row.createCell(3).setCellFormula("(A1+B1)*1.0"); // a formula that returns '10'

        Cell cell4 = row.createCell(4);       // numeric cell with a custom style
        CellStyle style4 = workbook.createCellStyle();
        style4.setDataFormat(df.getFormat("0.0000"));
        cell4.setCellStyle(style4);
        cell4.setCellValue(10); // formatted as '10.0000'

        row.createCell(5).setCellValue("10.0000");

        // autosize not-evaluated cells, formula cells are sized as if the result is 0
        for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);

        assertTrue(sheet.getColumnWidth(0) < sheet.getColumnWidth(1));  // width of '0' is less then width of '10'
        assertEquals(sheet.getColumnWidth(1), sheet.getColumnWidth(2)); // 10 and '10' should be sized equally
        assertEquals(sheet.getColumnWidth(3), sheet.getColumnWidth(0)); // formula result is unknown, the width is calculated  for '0'
        assertEquals(sheet.getColumnWidth(4), sheet.getColumnWidth(5)); // 10.0000 and '10.0000'

        // evaluate formulas and re-autosize
        evaluateWorkbook(workbook);

        for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);

        assertTrue(sheet.getColumnWidth(0) < sheet.getColumnWidth(1));  // width of '0' is less then width of '10'
        assertEquals(sheet.getColumnWidth(1), sheet.getColumnWidth(2)); // columns 1, 2 and 3 should have the same width
        assertEquals(sheet.getColumnWidth(2), sheet.getColumnWidth(3)); // columns 1, 2 and 3 should have the same width
        assertEquals(sheet.getColumnWidth(4), sheet.getColumnWidth(5)); // 10.0000 and '10.0000'
    }

    public void testBooleanCells(){
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();

        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue(0); // getCachedFormulaResult() returns 0 for not evaluated formula cells
        row.createCell(1).setCellValue(true);
        row.createCell(2).setCellValue("TRUE");
        row.createCell(3).setCellFormula("1 > 0"); // a formula that returns true

        // autosize not-evaluated cells, formula cells are sized as if the result is 0
        for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

        assertTrue(sheet.getColumnWidth(1) > sheet.getColumnWidth(0));  // 'true' is wider than '0'
        assertEquals(sheet.getColumnWidth(1), sheet.getColumnWidth(2));  // 10 and '10' should be sized equally
        assertEquals(sheet.getColumnWidth(3), sheet.getColumnWidth(0));  // formula result is unknown, the width is calculated  for '0'

        // evaluate formulas and re-autosize
        evaluateWorkbook(workbook);

        for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

        assertTrue(sheet.getColumnWidth(1) > sheet.getColumnWidth(0));  // 'true' is wider than '0'
        assertEquals(sheet.getColumnWidth(1), sheet.getColumnWidth(2));  // columns 1, 2 and 3 should have the same width
        assertEquals(sheet.getColumnWidth(2), sheet.getColumnWidth(3));  // columns 1, 2 and 3 should have the same width
    }

    public void testDateCells(){
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();
        DataFormat df = workbook.getCreationHelper().createDataFormat();

        CellStyle style1 = workbook.createCellStyle();
        style1.setDataFormat(df.getFormat("m"));

        CellStyle style3 = workbook.createCellStyle();
        style3.setDataFormat(df.getFormat("mmm"));

        CellStyle style5 = workbook.createCellStyle(); //rotated text
        style5.setDataFormat(df.getFormat("mmm/dd/yyyy"));

        Calendar calendar = Calendar.getInstance();
        calendar.set(2010, 0, 1); // Jan 1 2010

        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue(DateUtil.getJavaDate(0));   //default date

        Cell cell1 = row.createCell(1);
        cell1.setCellValue(calendar);
        cell1.setCellStyle(style1);
        row.createCell(2).setCellValue("1"); // column 1 should be sized as '1'

        Cell cell3 = row.createCell(3);
        cell3.setCellValue(calendar);
        cell3.setCellStyle(style3);
        row.createCell(4).setCellValue("Jan");

        Cell cell5 = row.createCell(5);
        cell5.setCellValue(calendar);
        cell5.setCellStyle(style5);
        row.createCell(6).setCellValue("Jan/01/2010");

        Cell cell7 = row.createCell(7);
        cell7.setCellFormula("DATE(2010,1,1)");
        cell7.setCellStyle(style3); // should be sized as 'Jan'

        // autosize not-evaluated cells, formula cells are sized as if the result is 0
        for (int i = 0; i < 8; i++) sheet.autoSizeColumn(i);

        assertEquals(sheet.getColumnWidth(2), sheet.getColumnWidth(1)); // date formatted as 'm'
        assertTrue(sheet.getColumnWidth(3) > sheet.getColumnWidth(1));  // 'mmm' is wider than 'm'
        assertEquals(sheet.getColumnWidth(4), sheet.getColumnWidth(3)); // date formatted as 'mmm'
        assertTrue(sheet.getColumnWidth(5) > sheet.getColumnWidth(3));  // 'mmm/dd/yyyy' is wider than 'mmm'
        assertEquals(sheet.getColumnWidth(6), sheet.getColumnWidth(5)); // date formatted as 'mmm/dd/yyyy'

        // YK: width of not-evaluated formulas that return data is not determined
        // POI seems to conevert '0' to Excel date which is the beginng of the Excel's date system

        // evaluate formulas and re-autosize
        evaluateWorkbook(workbook);

        for (int i = 0; i < 8; i++) sheet.autoSizeColumn(i);

        assertEquals(sheet.getColumnWidth(2), sheet.getColumnWidth(1)); // date formatted as 'm'
        assertTrue(sheet.getColumnWidth(3) > sheet.getColumnWidth(1));  // 'mmm' is wider than 'm'
        assertEquals(sheet.getColumnWidth(4), sheet.getColumnWidth(3)); // date formatted as 'mmm'
        assertTrue(sheet.getColumnWidth(5) > sheet.getColumnWidth(3));  // 'mmm/dd/yyyy' is wider than 'mmm'
        assertEquals(sheet.getColumnWidth(6), sheet.getColumnWidth(5)); // date formatted as 'mmm/dd/yyyy'
        assertEquals(sheet.getColumnWidth(4), sheet.getColumnWidth(7)); // date formula formatted as 'mmm'
    }

    public void testStringCells(){
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0);

        Font defaultFont = workbook.getFontAt((short)0);

        CellStyle style1 = workbook.createCellStyle();
        Font font1 = workbook.createFont();
        font1.setFontHeight((short)(2*defaultFont.getFontHeight()));
        style1.setFont(font1);

        row.createCell(0).setCellValue("x");
        row.createCell(1).setCellValue("xxxx");
        row.createCell(2).setCellValue("xxxxxxxxxxxx");
        row.createCell(3).setCellValue("Apache\nSoftware Foundation"); // the text is splitted into two lines
        row.createCell(4).setCellValue("Software Foundation");

        Cell cell5 = row.createCell(5);
        cell5.setCellValue("Software Foundation");
        cell5.setCellStyle(style1); // same as in column 4 but the font is twice larger than the default font

        for (int i = 0; i < 10; i++) sheet.autoSizeColumn(i);

        assertTrue(2*sheet.getColumnWidth(0) < sheet.getColumnWidth(1)); // width is roughly proportional to the number of characters
        assertTrue(2*sheet.getColumnWidth(1) < sheet.getColumnWidth(2));
        assertEquals(sheet.getColumnWidth(4), sheet.getColumnWidth(3));
        assertTrue(sheet.getColumnWidth(5) > sheet.getColumnWidth(4)); //larger font results in a wider column width
    }

    public void testRotatedText(){
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0);

        CellStyle style1 = workbook.createCellStyle();
        style1.setRotation((short)90);

        Cell cell0 = row.createCell(0);
        cell0.setCellValue("Apache Software Foundation");
        cell0.setCellStyle(style1);

        Cell cell1 = row.createCell(1);
        cell1.setCellValue("Apache Software Foundation");

        for (int i = 0; i < 2; i++) sheet.autoSizeColumn(i);

        int w0 = sheet.getColumnWidth(0);
        int w1 = sheet.getColumnWidth(1);

        assertTrue(w0*5 < w1); // rotated text occupies at least five times less horizontal space than normal text
    }

    public void testMergedCells(){
        Workbook workbook = _testDataProvider.createWorkbook();
        Sheet sheet = workbook.createSheet();

        Row row = sheet.createRow(0);
        sheet.addMergedRegion(CellRangeAddress.valueOf("A1:B1"));

        Cell cell0 = row.createCell(0);
        cell0.setCellValue("Apache Software Foundation");

        int defaulWidth = sheet.getColumnWidth(0);
        sheet.autoSizeColumn(0);
        // column is unchanged if merged regions are ignored (Excel like behavior)
        assertEquals(defaulWidth, sheet.getColumnWidth(0));

        sheet.autoSizeColumn(0, true);
        assertTrue(sheet.getColumnWidth(0) > defaulWidth);
    }

}