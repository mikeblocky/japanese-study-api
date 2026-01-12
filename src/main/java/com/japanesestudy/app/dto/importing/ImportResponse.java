package com.japanesestudy.app.dto.importing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for import operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {
    private String message;
    private int coursesCreated;
    private int topicsCreated;
    private int itemsImported;
    private int skippedItems;
    private List<String> warnings;
}
