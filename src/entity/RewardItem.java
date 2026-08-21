package entity;

/**
 * Author: Tan Hock Siang
 * Entity class representing a single item in the Loyalty & Rewards Catalog.
 * Stored via Binary Search Tree (BST) using itemId as the key.
 */
public class RewardItem implements Comparable<RewardItem> {

    private static int idCounter = 1;

    private String itemId;
    private String itemName;
    private int pointsCost;
    private int stockQuantity;
    private int defaultStockQuantity;
    private int totalRedeemed;
    private int validityMinutes;

    public RewardItem(String itemName, int pointsCost, int stockQuantity, int validityMinutes) {
        this.itemId = "RW" + String.format("%03d", idCounter++);
        this.itemName = itemName;
        this.pointsCost = pointsCost;
        this.stockQuantity = stockQuantity;
        this.defaultStockQuantity = stockQuantity;
        this.totalRedeemed = 0;
        this.validityMinutes = validityMinutes;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getPointsCost() {
        return pointsCost;
    }

    public void setPointsCost(int pointsCost) {
        this.pointsCost = pointsCost;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getDefaultStockQuantity() {
        return defaultStockQuantity;
    }

    public int getTotalRedeemed() {
        return totalRedeemed;
    }

    public int getValidityMinutes() {
        return validityMinutes;
    }

    public boolean hasStock() {
        return stockQuantity > 0;
    }

    /** Records one request that has completed FIFO settlement. */
    public void recordRedemptionCompleted() {
        totalRedeemed++;
    }

    public void restock(int quantity) {
        if (quantity > 0) {
            stockQuantity += quantity;
        }
    }

    public void resetStockToDefault() {
        this.stockQuantity = this.defaultStockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    @Override
    public int compareTo(RewardItem other) {
        return this.itemId.compareTo(other.itemId);
    }

}
