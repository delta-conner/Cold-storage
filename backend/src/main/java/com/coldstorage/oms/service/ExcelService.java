package com.coldstorage.oms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coldstorage.oms.common.BusinessException;
import com.coldstorage.oms.entity.ColdRoom;
import com.coldstorage.oms.entity.Device;
import com.coldstorage.oms.mapper.ColdRoomMapper;
import com.coldstorage.oms.mapper.DeviceMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ExcelService {

    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private ColdRoomMapper coldRoomMapper;
    @Autowired
    private DeviceService deviceService;

    public byte[] exportDevices() {
        List<Device> list = deviceMapper.selectList(new LambdaQueryWrapper<Device>().orderByAsc(Device::getId));
        Map<Long, ColdRoom> roomMap = coldRoomMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(ColdRoom::getId, r -> r, (a, b) -> a));
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("设备台账");
            String[] headers = {"设备编号", "设备名称", "设备类型", "品牌", "型号", "冷藏间编码", "是否公共", "状态",
                    "投用日期", "维保周期天", "额定参数", "备注"};
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                head.createCell(i).setCellValue(headers[i]);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            int r = 1;
            for (Device d : list) {
                Row row = sheet.createRow(r++);
                ColdRoom room = roomMap.get(d.getRoomId());
                row.createCell(0).setCellValue(nvl(d.getDeviceNo()));
                row.createCell(1).setCellValue(nvl(d.getDeviceName()));
                row.createCell(2).setCellValue(nvl(d.getDeviceType()));
                row.createCell(3).setCellValue(nvl(d.getBrand()));
                row.createCell(4).setCellValue(nvl(d.getModel()));
                row.createCell(5).setCellValue(room == null ? "" : nvl(room.getCode()));
                row.createCell(6).setCellValue(d.getIsPublic() != null && d.getIsPublic() == 1 ? "是" : "否");
                row.createCell(7).setCellValue(nvl(d.getStatus()));
                row.createCell(8).setCellValue(d.getInstallDate() == null ? "" : sdf.format(d.getInstallDate()));
                row.createCell(9).setCellValue(d.getMaintainCycleDays() == null ? 90 : d.getMaintainCycleDays());
                row.createCell(10).setCellValue(nvl(d.getRatedParams()));
                row.createCell(11).setCellValue(nvl(d.getRemark()));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }

    public Map<String, Object> importDevices(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Excel 文件");
        }
        Map<String, Long> roomCodeMap = new HashMap<>();
        for (ColdRoom r : coldRoomMapper.selectList(null)) {
            if (r.getCode() != null) {
                roomCodeMap.put(r.getCode(), r.getId());
            }
        }
        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                try {
                    String deviceNo = cell(row, 0);
                    String deviceName = cell(row, 1);
                    if (deviceNo.isEmpty() || deviceName.isEmpty()) {
                        fail++;
                        errors.add("第" + (i + 1) + "行：编号/名称不能为空");
                        continue;
                    }
                    String roomCode = cell(row, 5);
                    Long roomId = roomCodeMap.get(roomCode);
                    if (roomId == null) {
                        fail++;
                        errors.add("第" + (i + 1) + "行：冷藏间编码不存在 " + roomCode);
                        continue;
                    }
                    Device d = new Device();
                    Device exists = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceNo, deviceNo));
                    if (exists != null) {
                        d.setId(exists.getId());
                    }
                    d.setDeviceNo(deviceNo);
                    d.setDeviceName(deviceName);
                    d.setDeviceType(cell(row, 2));
                    d.setBrand(cell(row, 3));
                    d.setModel(cell(row, 4));
                    d.setRoomId(roomId);
                    d.setIsPublic("是".equals(cell(row, 6)) ? 1 : 0);
                    String st = cell(row, 7);
                    d.setStatus(st.isEmpty() ? "NORMAL" : st);
                    String dateStr = cell(row, 8);
                    if (!dateStr.isEmpty()) {
                        d.setInstallDate(sdf.parse(dateStr));
                    }
                    String cycle = cell(row, 9);
                    d.setMaintainCycleDays(cycle.isEmpty() ? 90 : Integer.parseInt(cycle.replace(".0", "")));
                    d.setRatedParams(cell(row, 10));
                    d.setRemark(cell(row, 11));
                    deviceService.save(d);
                    success++;
                } catch (Exception ex) {
                    fail++;
                    errors.add("第" + (i + 1) + "行：" + ex.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException("导入失败：" + e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors.size() > 20 ? errors.subList(0, 20) : errors);
        return result;
    }

    private String cell(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (c == null) {
            return "";
        }
        return new DataFormatter().formatCellValue(c).trim();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
