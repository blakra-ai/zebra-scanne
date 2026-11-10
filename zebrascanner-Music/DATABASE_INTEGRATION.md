# Database Integration - Shipped Orders

## Overview
Successfully integrated Room database to persist shipped orders locally and display them in the List tab.

## Database Structure

### Entity: ShippedOrderEntity
```kotlin
@Entity(tableName = "shipped_orders")
data class ShippedOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val trackingNumber: String,
    val shippedOn: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

### DAO: ShippedOrderDao
Available operations:
- `insertOrder(order)` - Save new shipped order
- `getAllOrders()` - Get all orders (Flow for reactive updates)
- `getOrderByTrackingNumber(trackingNumber)` - Find specific order
- `deleteOrder(id)` - Delete single order
- `deleteAllOrders()` - Clear all orders
- `getOrderCount()` - Get total count

### Database: AppDatabase
- Database name: `zebra_scanner_database`
- Version: 1
- Singleton pattern for thread-safe access

## Implementation Flow

### 1. When Order is Shipped (200 Response)
```
User submits tracking number
    ↓
API call to mark-shipped endpoint
    ↓
Success (200) response received
    ↓
Save to Room database
    ↓
Display success message
    ↓
Clear input field
```

### 2. Displaying Orders in List Tab
```
List tab opened
    ↓
Query database with Flow
    ↓
Reactive updates (auto-refresh when data changes)
    ↓
Display in scrollable list
```

## Features

### Home Screen
- ✅ Saves order to database on successful API response
- ✅ Stores: order number, tracking number, shipped date, message
- ✅ Auto-generated ID and timestamp

### List Screen
- ✅ Real-time updates using Flow
- ✅ Displays all shipped orders
- ✅ Shows order count
- ✅ Beautiful card-based UI
- ✅ Empty state with helpful message
- ✅ Formatted date display

### Order Card Information
Each order displays:
- Order number (bold title)
- Tracking number (primary color)
- Success message
- Shipped date (formatted)

## Database Location
- Path: `/data/data/com.example.zebrascanner/databases/zebra_scanner_database`
- Persists across app restarts
- Survives app updates (unless uninstalled)

## Future Enhancements
1. Add delete functionality (swipe to delete)
2. Add search/filter by tracking number
3. Add date range filtering
4. Export orders to CSV
5. Sync with server
6. Add order details screen
7. Add statistics/analytics

## Dependencies Added
```kotlin
// Room Database
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
```

## Usage Example

### Save Order
```kotlin
val database = AppDatabase.getDatabase(context)
val order = ShippedOrderEntity(
    orderNumber = "ord461715",
    trackingNumber = "t300",
    shippedOn = "2026-02-09T09:47:07.000000Z",
    message = "Order has been marked as shipped."
)
database.shippedOrderDao().insertOrder(order)
```

### Get All Orders
```kotlin
val database = AppDatabase.getDatabase(context)
val orders by database.shippedOrderDao()
    .getAllOrders()
    .collectAsState(initial = emptyList())
```

## Testing
1. Submit a tracking number in Home tab
2. Wait for success message
3. Switch to List tab
4. Order should appear immediately
5. Restart app - orders persist
6. Submit more orders - list updates automatically

## Notes
- Database operations run on background threads (coroutines)
- Flow provides automatic UI updates when data changes
- No manual refresh needed
- Thread-safe singleton pattern
- Auto-generated primary keys
