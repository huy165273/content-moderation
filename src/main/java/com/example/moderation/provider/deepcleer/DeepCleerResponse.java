package com.example.moderation.provider.deepcleer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response model cho DeepCleer API
 * Theo tài liệu chính thức: http://api-text-bj.fengkongcloud.com/text/v4
 *
 * Response codes:
 * - 1100: Success
 * - 1901: QPS limit exceeded
 * - 1902: Invalid parameters
 * - 1903: Service failure
 * - 1905: Character limit exceeded
 * - 9101: Unauthorized operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepCleerResponse {

    /**
     * Return code (Required)
     */
    private Integer code;

    /**
     * Description of the return code (Required)
     */
    private String message;

    /**
     * Request identifier (Required)
     * Used for troubleshooting and effect optimization
     */
    private String requestId;

    /**
     * Disposal recommendation (Required when code=1100)
     * Values: PASS, REVIEW, REJECT
     */
    private String riskLevel;

    /**
     * Primary risk label (Required when code=1100)
     * Returns 'normal' when riskLevel is PASS
     */
    private String riskLabel1;

    /**
     * Secondary risk label (Required when code=1100)
     * Empty when riskLevel is PASS
     */
    private String riskLabel2;

    /**
     * Tertiary risk label (Required when code=1100)
     * Empty when riskLevel is PASS
     */
    private String riskLabel3;

    /**
     * Risk reason (Required when code=1100)
     * Returns 'normal' when riskLevel is PASS
     */
    private String riskDescription;

    /**
     * Mapped risk details (Required when code=1100)
     */
    private Map<String, Object> riskDetail;

    /**
     * Account risk profile label information (Required when code=1100)
     */
    private Map<String, Object> tokenLabels;

    /**
     * Auxiliary information (Required when code=1100)
     */
    private AuxInfo auxInfo;

    /**
     * All matched risk labels and detailed information (Required when code=1100)
     */
    private List<AllLabel> allLabels;

    /**
     * All matched business labels and detailed information (Required when code=1100)
     */
    private List<Map<String, Object>> businessLabels;

    /**
     * Attribute account labels (Optional)
     */
    private List<Map<String, Object>> tokenProfileLabels;

    /**
     * Risk account labels (Optional)
     */
    private List<Map<String, Object>> tokenRiskLabels;

    /**
     * Language information (Optional)
     */
    private Map<String, Object> langResult;

    /**
     * Knowledge base details (Optional)
     */
    private Map<String, Object> kbDetail;

    /**
     * Whether the result is final (Required when code=1100)
     * 1: Can be used directly for disposal
     * 0: Requires further manual review
     */
    private Integer finalResult;

    /**
     * Whether the current result is from machine review or human review (Required when code=1100)
     * 0: Machine review
     * 1: Human review
     */
    private Integer resultType;

    /**
     * Disposal and mapping results (Optional)
     * Returned if custom label system is configured
     */
    private Map<String, Object> disposal;

    /**
     * Auxiliary information structure
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuxInfo {
        /**
         * Detected contact information
         */
        private List<ContactResult> contactResult;

        /**
         * Text with sensitive content filtered/masked
         */
        private String filteredText;
    }

    /**
     * Contact information detected in text
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactResult {
        /**
         * Contact string (e.g., "qq12345")
         */
        private String contactString;

        /**
         * Contact type
         * 2: QQ number
         */
        private Integer contactType;
    }

    /**
     * All labels structure
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AllLabel {
        /**
         * Confidence probability (0-1)
         */
        private Double probability;

        /**
         * Risk description
         */
        private String riskDescription;

        /**
         * Risk detail with matched lists
         */
        private RiskDetail riskDetail;

        /**
         * Primary risk label
         */
        private String riskLabel1;

        /**
         * Secondary risk label
         */
        private String riskLabel2;

        /**
         * Tertiary risk label
         */
        private String riskLabel3;

        /**
         * Risk level for this specific label
         */
        private String riskLevel;
    }

    /**
     * Risk detail structure
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RiskDetail {
        /**
         * Matched word lists
         */
        private List<MatchedList> matchedLists;
    }

    /**
     * Matched list structure
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchedList {
        /**
         * Name of the matched list (e.g., "Community Sensitive Word List")
         */
        private String name;

        /**
         * Matched words in the list
         */
        private List<MatchedWord> words;
    }

    /**
     * Matched word structure
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchedWord {
        /**
         * Position of the word in text [start, end]
         */
        private List<Integer> position;

        /**
         * The matched word
         */
        private String word;
    }
}
