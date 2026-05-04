package com.reports.api.dto;

/** Buffered .xlsx payload (avoids async streaming conflicts with Spring Security error responses). */
public record ExcelExportDescriptor(String fileName, byte[] content) {
}
