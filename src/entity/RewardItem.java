package entity;

/**
 * Author: Hock Siang
 * A reward catalog item stored in the custom BST by stable item ID.
 */
public class RewardItem implements Comparable<RewardItem> {
    public static final String IN_STOCK = "IN_STOCK";
    public static final String LOW_STOCK = "LOW_STOCK";
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";

    private static int nextId = 1;
    private final String itemId;
    private String itemName;
    private int pointsCost;
    private int stockQuantity;
    private int defaultStockQuantity;
    private int totalRedeemed;
    private int validityDays;
    private boolean active;

    public RewardItem(String itemName, int pointsCost, int stockQuantity, int validityDays) {
        this(String.format("RW%03d", nextId++), itemName, pointsCost, stockQuantity,
                validityDays, true);
    }

    public RewardItem(String itemId, String itemName, int pointsCost,
            int stockQuantity, int validityDays) {
        this(itemId, itemName, pointsCost, stockQuantity, validityDays, true);
    }

    public RewardItem(String itemId, String itemName, int pointsCost,
            int stockQuantity, int validityDays, boolean active) {
        this.itemId = itemId == null ? "" : itemId.trim().toUpperCase();
        this.itemName = itemName == null ? "" : itemName.trim();
        this.pointsCost = Math.max(0, pointsCost);
        this.stockQuantity = Math.max(0, stockQuantity);
        this.defaultStockQuantity = this.stockQuantity;
        this.totalRedeemed = 0;
        this.validityDays = Math.max(1, validityDays);
        this.active = active;
    }

    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) {
        if (itemName != null && !itemName.trim().isEmpty()) this.itemName = itemName.trim();
    }
    public int getPointsCost() { return pointsCost; }
    public void setPointsCost(int pointsCost) { if (pointsCost >= 0) this.pointsCost = pointsCost; }
    public int getStockQuantity() { return stockQuantity; }
    public int getDefaultStockQuantity() { return defaultStockQuantity; }
    public int getTotalRedeemed() { return totalRedeemed; }
    public int getValidityDays() { return validityDays; }
    public void setValidityDays(int validityDays) { if (validityDays > 0) this.validityDays = validityDays; }
    public int getValidityMinutes() { return validityDays * 24 * 60; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean hasStock() { return active && stockQuantity > 0; }

    public boolean recordRedemption() {
        if (!hasStock()) return false;
        stockQuantity--;
        totalRedeemed++;
        return true;
    }

    public void recordRedeemed() { recordRedemption(); }
    public void restock(int quantity) { if (quantity > 0) stockQuantity += quantity; }
    public void resetStockToDefault() { stockQuantity = defaultStockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = Math.max(0, stockQuantity); }

    public String getStockStatus() {
        if (stockQuantity == 0) return OUT_OF_STOCK;
        int lowThreshold = Math.max(1, defaultStockQuantity / 3);
        return stockQuantity <= lowThreshold ? LOW_STOCK : IN_STOCK;
    }

    @Override
    public int compareTo(RewardItem other) {
        if (other == null) return 1;
        return itemId.compareToIgnoreCase(other.itemId);
    }

    @Override
    public String toString() {
        return String.format("%-5s | %-30s | Cost: %4d pts | Stock: %3d | Valid: %3d days",
                itemId, itemName, pointsCost, stockQuantity, validityDays);
    }
}
