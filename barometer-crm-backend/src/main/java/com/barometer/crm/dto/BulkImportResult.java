package com.barometer.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkImportResult {
    private int total;
    private int imported;
    private int skipped;
    private List<String> errors;
}
