# TARUMT Resort Management System — 完整代码教学 / Code Walkthrough

---

# 第一部分：整个 Program 总览

## 1. 这个 Program 整体是做什么的？

这是一个 **TARUMT 度假村管理系统 (Resort Management System)**。
它模拟一间度假村酒店的日常运营，包括：客人预订房间、前台办理入住/退房、客房清洁管理、以及会员忠诚度奖励。

## 2. Program 的主要功能

| 模块编号 | 模块名称 | 负责人 | 主要功能 |
|---------|---------|--------|---------|
| 1 | Walk-In & Standard Booking | Zhi Xuan | 客人排队登记、分配房间、创建预订、取消预订 |
| 2 | Housekeeping & Task Log | Kai Wei | 管理房间清洁状态、推进/回滚清洁流程、任务日志 |
| 3 | Front-Desk Service System | Wei Sheng | 搜索客人、办理入住/退房、换房、生成账单、管理报告 |
| 4 | Loyalty & Rewards Service | Hock Siang | (Placeholder，尚未完成) |

## 3. Program 从哪里开始执行？

从 [App.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/App.java) 的 `main()` 方法开始。

## 4. Main Class 是哪一个？

**`App.java`** — 这是唯一的入口点 (Entry Point)。

## 5. 从 Main 开始，整个 Program 的执行流程

```
Program 启动
    ↓
App.main()
    ↓
初始化共享数据 (Master Guest Registry BST + Shared Room List)
    ↓
seedMasterData() → 预先填入 Guest 和 Room 数据
    ↓
创建 4 个 UI 子系统（BookingUI, HousekeepingUI, FrontDeskUI, LoyaltyUI）
    ↓
显示主菜单 (do-while 循环)
    ↓
用户选择 1/2/3/4 → 进入对应模块的 displayMenu()
    ↓
在子模块内操作 → 完成后返回主菜单
    ↓
用户选择 0 → 退出程序
```

## 6. 不同 Class 之间的连接关系

```
App (主入口)
 ├── 创建共享数据: BinarySearchTree<Guest> + MyArrayList<Room>
 │
 ├── BookingUI (boundary，UI 界面)
 │    └── BookingController (control，业务逻辑)
 │         ├── ArrayQueue<Guest> (ADT: 排队用)
 │         ├── MyArrayList<Booking> (ADT: 存预订记录)
 │         ├── MyArrayList<Room> (ADT: 共享房间列表)
 │         └── BinarySearchTree<Guest> (ADT: 共享 Guest Registry)
 │
 ├── HousekeepingUI (boundary)
 │    └── HousekeepingController (control)
 │         ├── MyArrayList<Room> (ADT: 共享房间列表)
 │         └── ArrayStack<HousekeepingLog> (ADT: 任务日志栈)
 │
 ├── FrontDeskUI (boundary)
 │    └── FrontDeskController (control)
 │         ├── BinarySearchTree<Guest> (ADT: 共享 Guest BST)
 │         ├── BinarySearchTree<Room> (ADT: Room BST，内部同步)
 │         └── MyArrayList<Room> (ADT: 共享房间列表)
 │
 └── LoyaltyUI (boundary, Placeholder)
      └── LoyaltyController (control, Placeholder)
           └── BinarySearchTree<Guest> (ADT: 共享 Guest BST)
```

### 关键架构概念：共享内存 (Shared Memory)

> [!IMPORTANT]
> 这个 Program 最重要的设计是：**所有模块共享同一份数据**。
> 
> `App.main()` 创建了 **一棵 Guest BST** 和 **一个 Room List**，然后把它们的**引用 (reference)** 传给所有 UI/Controller。
> 
> 这意味着：如果 Booking 模块把一个房间标为 "Reserved"，Front Desk 模块立刻可以看到这个变化，因为它们操作的是**同一个对象**。

### 项目分层结构 (MVC 分层)

```
┌─────────────────────────────────────┐
│  boundary (UI 界面层)               │  ← 负责与用户交互、显示菜单、读取输入
│  BookingUI / FrontDeskUI / etc.     │
├─────────────────────────────────────┤
│  control (控制器/业务逻辑层)         │  ← 负责处理业务规则、操作数据
│  BookingController / etc.           │
├─────────────────────────────────────┤
│  entity (实体/数据模型层)            │  ← 代表真实世界的对象 (Guest, Room, Booking)
│  Guest / Room / Booking / etc.      │
├─────────────────────────────────────┤
│  adt (抽象数据类型层)                │  ← 自定义 Data Structure 实现
│  MyArrayList / ArrayQueue /         │
│  ArrayStack / BinarySearchTree      │
└─────────────────────────────────────┘
```

---

# 第二部分：逐个解释每一个 Class

---

## Entity (实体) Classes

---

### 1. [Guest.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/entity/Guest.java)

**用途：**
→ 代表一个度假村的客人 (Guest)，同时也代表一次住宿记录 (Stay Record)。

**为什么需要这个 Class？**
→ 系统需要记录每一位客人的身份信息、预订状态、房间分配、忠诚度等级等。这个 Class 把所有跟一个客人相关的数据打包在一起。

**里面储存：**

| 字段 | 类型 | 用途 |
|------|------|------|
| `guestName` | String | 客人名字 |
| `icNo` | String | IC / Passport 号码 (身份证) |
| `phoneNumber` | String | 电话号码 |
| `gender` | String | 性别 |
| `nationality` | String | 国籍 |
| `email` | String | 电子邮件 |
| `confirmationNumber` | String | 8 位确认号（**BST 的排序 Key**） |
| `bookingStatus` | String | 预订状态: Reserved / CheckedIn / CheckedOut / Cancelled |
| `checkInDate` | String | 入住日期 |
| `checkOutDate` | String | 退房日期 |
| `numberOfNights` | int | 住几晚 |
| `assignedRoomNumber` | String | 被分配的房间号 |
| `roomType` | String | 房间类型 |
| `roomRate` | double | 每晚房价 |
| `loyaltyTier` | String | 会员等级: Platinum / Gold / Silver / Standard |
| `loyaltyPoints` | int | 会员积分 |
| `specialRequest` | String | 特殊要求 (如 "Extra pillows") |

**主要负责：**
→ 保存客人的所有资料，提供 getter/setter，以及实现 `Comparable<Guest>` 接口让 BST 可以用 `confirmationNumber` 来排序。

**它与其他 Class 的关系：**
→ 被 **所有模块** 使用。BST (`BinarySearchTree`) 用 `confirmationNumber` 作为 Key 来储存 Guest。

**哪些 Class 会使用它：**
→ `BookingController`, `FrontDeskController`, `LoyaltyController`, `BookingUI`, `FrontDeskUI`, `App`

---

### 2. [Room.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/entity/Room.java)

**用途：**
→ 代表度假村的一间客房。

**为什么需要这个 Class？**
→ 系统需要跟踪每间房的房号、类型、清洁状态和价格。

**里面储存：**

| 字段 | 类型 | 用途 |
|------|------|------|
| `roomNumber` | String | 房间号 (如 "101") |
| `roomType` | String | Deluxe Suite / Presidential Suite / Standard Room |
| `roomStatus` | String | Dirty / Cleaning In Progress / Inspected / Ready for Check-In / Occupied / Reserved |
| `price` | double | 每晚房价 |

**主要负责：**
→ 保存一间房的状态。也实现了 `Comparable<Room>` 接口，让 Room BST 可以用 `roomNumber` 排序。

**哪些 Class 会使用它：**
→ `BookingController`, `FrontDeskController`, `HousekeepingController`，以及所有 UI 类。

---

### 3. [Booking.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/entity/Booking.java)

**用途：**
→ 代表一笔预订记录。

**为什么需要这个 Class？**
→ 当一个 Guest 从等待队列中被处理 (Dequeue) 并分配房间后，系统需要产生一个 Booking 记录来追踪。

**里面储存：**

| 字段 | 类型 | 用途 |
|------|------|------|
| `bookingId` | String | 预订编号 (如 "BK0001") |
| `guestConfirmationNumber` | String | 关联到哪个 Guest |
| `guestName` | String | 客人名字 |
| `roomNumber` | String | 房间号 |
| `roomType` | String | 房间类型 |
| `roomPrice` | double | 每晚价格 |
| `checkInDate` | String | 入住日期 |
| `numberOfNights` | int | 住几晚 |
| `bookingStatus` | String | Confirmed / Cancelled |

**哪些 Class 会使用它：**
→ 只在 `BookingController` 和 `BookingUI` 中使用。

---

### 4. [HousekeepingLog.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/entity/HousekeepingLog.java)

**用途：**
→ 记录一次房间清洁状态的变更。

**为什么需要这个 Class？**
→ 每次修改房间清洁状态时，系统需要记录这次变更的详情（哪间房、之前什么状态、改成什么状态、谁做的、什么时间），以便支持**撤销 (Rollback)** 功能。

**里面储存：**

| 字段 | 类型 | 用途 |
|------|------|------|
| `taskId` | int | 任务编号 (自动递增) |
| `roomNumber` | String | 哪间房 |
| `previousStatus` | String | 改之前的状态 |
| `newStatus` | String | 改之后的状态 |
| `staffName` | String | 操作人员名字 |
| `timestamp` | String | 操作时间 |

**哪些 Class 会使用它：**
→ `HousekeepingController` (放入 Stack)，`HousekeepingUI` (显示)。

---

## ADT (Abstract Data Type，抽象数据类型) Classes

---

### 5. [ListInterface.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/adt/ListInterface.java)

**用途：**
→ 定义 List（列表）的操作接口 (Interface)。

**为什么需要？**
→ 这是 **面向接口编程 (Program to Interface)**。定义了 `add`, `get`, `getNumberOfEntries`, `isEmpty`, `clear`, `sort` 这些操作，但不规定具体怎么实现。

**定义的方法：**
`add(T)`, `get(int)`, `getNumberOfEntries()`, `isEmpty()`, `clear()`, `sort(Comparator)`

---

### 6. [MyArrayList.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/adt/MyArrayList.java)

**用途：**
→ **自定义的 Array-Based List 实现**（类似 Java 内建的 `java.util.ArrayList`，但是自己写的）。

**为什么需要？**
→ 这是 DSA 作业要求——必须自己实现 Data Structure，不能直接用 Java 内建的。

**里面储存：**
- `array` — 底层数组 (Object[])
- `numberOfEntries` — 当前有多少元素
- `DEFAULT_CAPACITY = 25` — 初始容量

**关键特点：**
- 当数组满了，会 `doubleCapacity()` 自动扩容（容量翻倍）
- `sort()` 使用 **Selection Sort（选择排序）** 算法

**被用在哪里：**
→ 整个 Program 到处都在用。储存 Room List、Booking List、Registered Guests、各种 Report 的筛选结果等。

---

### 7. [QueueInterface.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/adt/QueueInterface.java)

**用途：**
→ 定义 Queue（队列，FIFO 先进先出）的操作接口。

**定义的方法：**
`enqueue(T)`, `dequeue()`, `getFront()`, `isEmpty()`, `getNumberOfEntries()`, `clear()`, `toList()`

---

### 8. [ArrayQueue.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/adt/ArrayQueue.java)

**用途：**
→ **自定义的 Circular Array Queue 实现**（环形数组队列）。

**为什么需要？**
→ Booking 模块需要让客人排队等待——先来的先服务 (FIFO)。

**里面储存：**
- `array` — 底层数组
- `front` — 队列前端指针
- `rear` — 队列后端指针
- `numberOfEntries` — 当前元素数量

**关键特点：**
- 使用 **Circular Array（环形数组）** 技术，`front` 和 `rear` 用 `% array.length` 取模来实现循环
- `enqueue` 和 `dequeue` 都是 **O(1)** 操作
- `toList()` 可以在不修改 Queue 的情况下拿到所有元素的快照

**被用在哪里：**
→ `BookingController.waitingQueue` — 管理等待分配房间的客人

---

### 9. [StackInterface.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/adt/StackInterface.java)

**用途：**
→ 定义 Stack（栈，LIFO 后进先出）的操作接口。

**定义的方法：**
`push(T)`, `pop()`, `peek()`, `isEmpty()`, `getNumberOfEntries()`, `clear()`, `toList()`

---

### 10. [ArrayStack.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/adt/ArrayStack.java)

**用途：**
→ **自定义的 Array-Based Stack 实现**。

**为什么需要？**
→ Housekeeping 模块需要记录每次状态变更，并支持「撤销最近一次操作」。Stack 的 LIFO 特性完美符合——最后的操作最先被撤销。

**里面储存：**
- `array` — 底层数组
- `numberOfEntries` — 栈内元素数量

**关键特点：**
- `push`, `pop`, `peek` 都是 **O(1)** 操作
- `toList()` 返回从栈顶到栈底的顺序（最新的在前面）

**被用在哪里：**
→ `HousekeepingController.taskLogStack` — 记录清洁状态变更日志

---

### 11. [BSTInterface.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/adt/BSTInterface.java)

**用途：**
→ 定义 Binary Search Tree（二叉搜索树）的操作接口。

**定义的方法：**
`add`, `remove`, `search`, `contains`, `rangeSearch`, `getMin`, `getMax`, `getHeight`, `inOrderTraversal`, `preOrderTraversal`, `postOrderTraversal`, `rebalance`, `getLeafCount`, `isBalanced`, `printTree`, `getNumberOfEntries`, `isEmpty`, `clear`

---

### 12. [BinarySearchTree.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/adt/BinarySearchTree.java)

**用途：**
→ **自定义的 BST（二叉搜索树）实现**。这是整个 Program 中最重要的非线性 Data Structure。

**为什么需要？**
→ 用来管理 Guest Registry。用 `confirmationNumber` 作为排序 Key，实现 **O(log n) 的搜索效率**，比线性搜索 O(n) 快得多。

**里面储存：**
- `root` — 树根节点
- `numberOfEntries` — 树中总节点数
- 内部类 `Node<T>` — 每个节点包含 `data`, `left`, `right`

**关键特点：**
- 支持 **增 (add)**、**删 (remove)**、**查 (search)**、**范围查询 (rangeSearch)**
- 支持三种遍历：In-Order / Pre-Order / Post-Order
- 支持 **Rebalance（重新平衡）**
- 删除节点处理了三种情况：叶子节点、一个子节点、两个子节点（用 In-Order Successor 替换）

**被用在哪里：**
→ `App` 中作为 `masterGuestRegistry`；`FrontDeskController` 中作为 `guestTree` 和 `roomTree`

---

## Boundary (UI 界面) Classes

---

### 13. [BookingUI.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/boundary/BookingUI.java)

**用途：**
→ Walk-In & Booking 模块的用户界面。显示菜单、收集输入、展示结果。

**它不做业务逻辑**，只负责：
1. 显示选项菜单
2. 读取用户输入
3. 调用 `BookingController` 的方法
4. 显示操作结果

**哪些 Class 会使用它：**
→ `App.main()` 创建它并调用 `displayMenu()`

---

### 14. [HousekeepingUI.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/boundary/HousekeepingUI.java)

**用途：**
→ Housekeeping 模块的用户界面。

**主要功能：**
→ 查看房间状态、推进清洁流程、手动设置状态、回滚操作、生成报告。

---

### 15. [FrontDeskUI.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/boundary/FrontDeskUI.java)

**用途：**
→ Front Desk 模块的用户界面。这是**功能最丰富**的 UI，有 760 行代码。

**主要功能：**
→ 搜索客人（按确认号/名字/IC/范围）、注册/删除客人、入住、换房、生成账单、报告、BST 诊断。

---

### 16. [LoyaltyUI.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/boundary/LoyaltyUI.java)

**用途：**
→ Loyalty 模块的占位符 (Placeholder)。目前只显示一条通知信息。

---

## Control (控制器) Classes

---

### 17. [BookingController.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/control/BookingController.java)

**用途：**
→ 处理 Walk-In & Booking 的所有业务逻辑。

**里面储存：**
- `waitingQueue` (ArrayQueue\<Guest\>) — 等待队列
- `bookingList` (MyArrayList\<Booking\>) — 所有预订记录
- `registeredGuests` (MyArrayList\<Guest\>) — 已登记的客人列表
- `roomList` (MyArrayList\<Room\>) — 共享房间列表
- `masterGuestRegistry` (BST\<Guest\>) — 共享 Guest BST
- `nextConfirmationNumber`, `nextBookingId` — 自动递增的编号

---

### 18. [FrontDeskController.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/control/FrontDeskController.java)

**用途：**
→ 处理 Front Desk 的所有业务逻辑。

**里面储存：**
- `guestTree` (BST\<Guest\>) — 共享 Guest BST
- `roomTree` (BST\<Room\>) — Room BST（从 sharedRoomList 同步）
- `sharedRoomList` (MyArrayList\<Room\>) — 共享房间列表
- `activeCheckedInConfirmations` (MyArrayList\<String\>) — 当前已入住的确认号列表

---

### 19. [HousekeepingController.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/control/HousekeepingController.java)

**用途：**
→ 处理 Housekeeping 的所有业务逻辑。

**里面储存：**
- `roomList` (MyArrayList\<Room\>) — 共享房间列表
- `taskLogStack` (ArrayStack\<HousekeepingLog\>) — 清洁任务日志栈
- `STATUS_SEQUENCE` — 清洁流程固定顺序: `Dirty → Cleaning In Progress → Inspected → Ready for Check-In`

---

### 20. [LoyaltyController.java](file:///Users/WS/Desktop/DEGREE/DSA%20Assignment/src/control/LoyaltyController.java)

**用途：**
→ Loyalty 模块的占位符。只保存了 `masterGuestRegistry` 的引用。

---

# 第三部分：逐个解释每一个 Function / Method

---

## App.java 的 Functions

---

### Function: `main(String[] args)`

**【1. 做什么？】** 整个程序的入口点，初始化所有共享数据和 UI 模块，然后显示主菜单循环。

**【2. 为什么需要？】** Java 程序必须有 `main` 方法作为起点。

**【3. Input】** `String[] args` — 命令行参数（本程序未使用）

**【4. Output】** `void` — 不返回任何值，因为它是最顶层的入口。

**【5. 谁调用？】** JVM (Java Virtual Machine) 在程序启动时自动调用。

**【6. 执行过程】**

Step 1 → 创建 Scanner 对象用于读取用户输入  
Step 2 → 创建 `masterGuestRegistry`（BST）和 `sharedRoomList`（MyArrayList）  
Step 3 → 调用 `seedMasterData()` 预填数据  
Step 4 → 创建 4 个 UI 对象，把共享数据的引用传给它们  
Step 5 → 进入 `do-while` 循环显示主菜单  
Step 6 → 用 `switch` 根据用户选择调用不同 UI 的 `displayMenu()`  
Step 7 → 用户选 0 时退出循环，程序结束  

**【7. Data Structure】** BST (`BinarySearchTree<Guest>`) 和 MyArrayList (`MyArrayList<Room>`)

**【8. Algorithm】** 无特殊算法，就是 menu-driven loop

**【9. Time Complexity】** O(1) per iteration（每次循环是常数时间）

**【10. Space Complexity】** O(G + R) — G = 客人数，R = 房间数

**【11. 例子】** 
用户看到主菜单，输入 `3`，进入 FrontDeskUI 的菜单；操作完毕后返回主菜单；输入 `0` 退出。

**【12. 注意事项】** 
- 如果用户输入不是数字，`Integer.parseInt()` 会抛出异常，被 `catch` 捕获后 `choice = -1`，显示 "Invalid selection"。

---

### Function: `seedMasterData(BSTInterface<Guest>, ListInterface<Room>)`

**【1. 做什么？】** 预先填入初始测试数据（7 间房和 6 个客人）。

**【2. 为什么需要？】** 程序启动时需要一些数据来演示功能，不然系统是空的，什么都做不了。

**【3. Input】**
- `guestTree` — Guest BST 的引用
- `roomList` — Room List 的引用

**【4. Output】** `void` — 直接修改传入的数据结构，不需要返回。

**【5. 谁调用？】** `App.main()` 在初始化阶段调用一次。

**【6. 执行过程】**

Step 1 → 添加 7 个 Room 到 roomList（用 `roomList.add(new Room(...))`）  
Step 2 → 创建 Alice（已入住状态）并加入 BST  
Step 3 → 添加 Bob, Charlie, David, Eva, Frank 到 BST  

**【7. Data Structure】** MyArrayList（存 Room）、BinarySearchTree（存 Guest）

**【8. Algorithm】** BST Insertion — 每次 `guestTree.add()` 都按 confirmationNumber 插入到正确位置

**【9. Time Complexity】** O(G log G) — 每个 Guest 插入 BST 花费 O(log G)

**【11. 例子】**

插入顺序：Alice(10000001), Bob(10000002), Charlie(10000003), David(10000004), Eva(10000005), Frank(10000006)

BST 结构（按 confirmationNumber 排序）：
```
        10000001 (Alice)
             \
          10000002 (Bob)
               \
            10000003 (Charlie)
                 \
              10000004 (David)
                   \
                10000005 (Eva)
                     \
                  10000006 (Frank)
```
（注意：因为是按顺序插入的，BST 会退化成链表形状，直到调用 `rebalance()`）

---

## BinarySearchTree.java 的 Functions

---

### Function: `add(T newEntry)`

**【1. 做什么？】** 把一个新元素插入到 BST 的正确位置。

**【2. 为什么需要？】** 注册新 Guest 时，需要把 Guest 加入 BST。

**【3. Input】** `T newEntry` — 要插入的元素（比如一个 Guest 对象）

**【4. Output】** `boolean` — 成功返回 `true`，如果 `newEntry` 是 null 返回 `false`

**【5. 谁调用？】** `App.seedMasterData()`, `BookingController.registerWalkInGuest()`, `FrontDeskController.registerGuest()`, `FrontDeskController.syncRoomTree()`

**【6. 执行过程】**

Step 1 → 检查 `newEntry` 是否为 null，是则返回 false  
Step 2 → 调用 `addNode(root, newEntry)` 递归插入  
Step 3 → `numberOfEntries++`  

`addNode()` 的递归过程：
- 如果当前节点为 null → 创建新节点返回
- 比较 `newEntry` 和当前节点：
  - 如果更小 → 往左子树递归
  - 如果更大或相等 → 往右子树递归
- 返回当前节点

**【7. Data Structure】** BST (Binary Search Tree)

**【8. Algorithm】** BST Insertion（递归）

**【9. Time Complexity】** 
- 平均: O(log n) — 每次比较排除一半节点
- 最坏: O(n) — 当 BST 退化成链表（顺序插入时）

**【11. 例子】**

BST 现有：
```
      Bob(10000002)
     /            \
Alice(10000001)  Charlie(10000003)
```

插入 David(10000004)：
1. 比较 10000004 vs 10000002 → 大，去右边
2. 比较 10000004 vs 10000003 → 大，去右边
3. 右边是 null → 创建新节点
```
      Bob(10000002)
     /            \
Alice(10000001)  Charlie(10000003)
                       \
                    David(10000004)
```

---

### Function: `remove(T entry)`

**【1. 做什么？】** 从 BST 中删除指定的元素。

**【2. 为什么需要？】** Front Desk 需要删除客人记录。

**【3. Input】** `T entry` — 要删除的目标（用 confirmationNumber 匹配）

**【4. Output】** `T` — 返回被删除的元素；如果没找到返回 `null`

**【5. 谁调用？】** `FrontDeskController.removeGuest()`

**【6. 执行过程】**

Step 1 → 检查 entry 或 root 是否为 null  
Step 2 → 用一个长度为 1 的数组 `removedValue[0]` 来传递被删除的值（因为 Java 不支持 pass-by-reference）  
Step 3 → 调用 `removeNode()` 递归查找并删除  

`removeNode()` 处理三种删除情况：
- **Case 1: 叶子节点或只有一个子节点** → 直接用子节点替代
- **Case 2: 两个子节点** → 找到右子树中最小的值（In-Order Successor），用它替代被删除的节点，然后递归删除那个 successor

**【7. Data Structure】** BST

**【8. Algorithm】** BST Deletion + In-Order Successor

**【9. Time Complexity】** O(log n) 平均，O(n) 最坏

**【11. 例子】**

删除有两个子节点的 Bob(10000002)：
```
之前:      Bob(10000002)
          /            \
   Alice(10000001)  Charlie(10000003)

Step 1: 找到 Bob 的 In-Order Successor = Charlie(10000003)（右子树最小值）
Step 2: 把 Bob 的 data 替换为 Charlie
Step 3: 递归删除右子树中的 Charlie

之后:     Charlie(10000003)
          /
   Alice(10000001)
```

**【12. 注意事项】** 使用 `T[] removedValue` 数组来传递删除结果，这是因为 Java 的 primitive/reference 不支持真正的 pass-by-reference。

---

### Function: `search(T entry)`

**【1. 做什么？】** 在 BST 中搜索指定元素。

**【2. 为什么需要？】** 根据 confirmationNumber 快速查找 Guest。

**【3. Input】** `T entry` — 搜索目标（只需要 confirmationNumber 匹配）

**【4. Output】** `T` — 找到就返回该元素，没找到返回 `null`

**【5. 谁调用？】** `FrontDeskController.searchGuestByConfirmationNumber()`, `FrontDeskController.searchRoomByNumber()`, `BookingController.cancelBooking()`, `BinarySearchTree.contains()`

**【6. 执行过程】**

`searchNode()` 递归过程：
- 如果当前节点为 null → 返回 null（没找到）
- 比较目标和当前节点：
  - 相等 → 返回当前节点的 data
  - 目标更小 → 搜索左子树
  - 目标更大 → 搜索右子树

**【7. Data Structure】** BST

**【8. Algorithm】** BST Search（二分搜索的树版本）

**【9. Time Complexity】** O(log n) 平均，O(n) 最坏

**【11. 例子】**

搜索 10000003:
```
        10000002
       /        \
  10000001    10000004
              /
         10000003

Step 1: 比 10000002 大 → 去右边
Step 2: 比 10000004 小 → 去左边
Step 3: 等于 10000003 → 找到！返回这个 Guest
```

---

### Function: `rangeSearch(T minEntry, T maxEntry)`

**【1. 做什么？】** 找出 BST 中在 [minEntry, maxEntry] 范围内的所有元素。

**【2. 为什么需要？】** Front Desk 的 "Search by Confirmation Number Range" 功能。

**【3. Input】** `minEntry` (下界), `maxEntry` (上界)

**【4. Output】** `ListInterface<T>` — 包含所有在范围内的元素（按排序顺序）

**【5. 谁调用？】** `FrontDeskController.searchGuestsByConfirmationRange()`

**【6. 执行过程】**

`rangeSearchHelper()` 递归过程：
- 如果当前节点为 null → 返回
- 如果 minEntry < 当前节点 → 左子树可能有符合的，递归左边
- 如果 minEntry ≤ 当前节点 ≤ maxEntry → 把当前节点加入结果
- 如果 maxEntry > 当前节点 → 右子树可能有符合的，递归右边

**【7. Data Structure】** BST

**【8. Algorithm】** BST Range Search — 一种 **剪枝优化的 In-Order Traversal**

**【9. Time Complexity】** O(log n + k)，其中 k 是匹配结果数量

**【11. 例子】**

范围搜索 [10000002, 10000004]:
```
        10000001
             \
          10000002 ← 在范围内
               \
            10000003 ← 在范围内
                 \
              10000004 ← 在范围内
                   \
                10000005 ← 不在范围，不继续

结果: [Bob, Charlie, David]
```

**【12. 注意事项】** 与 In-Order Traversal 不同，Range Search 会 **跳过不可能包含结果的子树**，所以效率更高。

---

### Function: `inOrderTraversal()`

**【1. 做什么？】** 按照 Left → Root → Right 的顺序访问所有节点，返回排序后的列表。

**【2. 为什么需要？】** 需要按排序顺序获取所有 Guest（例如生成报告、搜索 by Name 等）。

**【3. Input】** 无

**【4. Output】** `ListInterface<T>` — 所有元素按升序排列

**【5. 谁调用？】** `FrontDeskController.searchGuestsByName()`, `FrontDeskController.searchGuestByIC()`, `FrontDeskController.getAllRooms()`, `BookingController.findGuestByIC()`, `BinarySearchTree.rebalance()`, 以及各种报告方法。

**【6. 执行过程】**

`inOrder()` 递归：
- 递归访问左子树
- 把当前节点加入列表
- 递归访问右子树

**【7. Data Structure】** BST + MyArrayList（结果存入）

**【8. Algorithm】** In-Order Traversal（中序遍历）

**【9. Time Complexity】** O(n) — 每个节点访问一次

**【11. 例子】**

```
      B
     / \
    A   C

In-Order: A → B → C （字母顺序）
```

---

### Function: `preOrderTraversal()`

**【1. 做什么？】** 按照 Root → Left → Right 的顺序访问。

**【2. 为什么需要？】** 用于观察 BST 的结构形状（先访问根，再看子树）。

**【9. Time Complexity】** O(n)

**【11. 例子】**
```
      B
     / \
    A   C

Pre-Order: B → A → C （根先出来）
```

---

### Function: `postOrderTraversal()`

**【1. 做什么？】** 按照 Left → Right → Root 的顺序访问。

**【2. 为什么需要？】** 用于自底向上的操作（先处理子节点，再处理父节点）。

**【9. Time Complexity】** O(n)

**【11. 例子】**
```
      B
     / \
    A   C

Post-Order: A → C → B （根最后出来）
```

---

### Function: `rebalance()`

**【1. 做什么？】** 把一棵可能不平衡的 BST 重新调整成高度平衡的 BST。

**【2. 为什么需要？】** 当按顺序插入时，BST 会退化成链表（如 seedMasterData 那样），搜索效率从 O(log n) 退化为 O(n)。Rebalance 可以恢复效率。

**【3. Input】** 无

**【4. Output】** `void` — 直接修改自身的 `root`

**【5. 谁调用？】** `FrontDeskController.rebalanceTrees()` → `FrontDeskUI` 的 "Rebalance Binary Search Trees" 选项

**【6. 执行过程】**

Step 1 → 用 `inOrderTraversal()` 取得所有元素的排序列表  
Step 2 → 用 `buildBalancedTree()` 递归重建：
  - 每次取中间元素作为根
  - 左半部分递归建左子树
  - 右半部分递归建右子树

**【7. Data Structure】** BST + MyArrayList

**【8. Algorithm】** Divide and Conquer（分治法）— 类似 Binary Search 的思路

**【9. Time Complexity】** O(n)

**【10. Space Complexity】** O(n) — 需要一个排序列表

**【11. 例子】**

Rebalance 前（链表形状）:
```
A
 \
  B
   \
    C
     \
      D
       \
        E
         \
          F
Height = 6
```

Rebalance 后（平衡形状）:
```
         C
        / \
       B   E
      /   / \
     A   D   F
Height = 3
```

---

### Function: `getMin()` / `getMax()`

**【1. 做什么？】** 找到 BST 中最小/最大的元素。

**【6. 执行过程】**
- `getMin()`: 一直往左走到底
- `getMax()`: 一直往右走到底

**【9. Time Complexity】** O(h)，h = 树的高度

---

### Function: `isBalanced()`

**【1. 做什么？】** 检查 BST 是否高度平衡（每个节点的左右子树高度差 ≤ 1）。

**【5. 谁调用？】** `FrontDeskController.getGuestTreeDiagnostics()`

**【8. Algorithm】** 递归检查每个节点的左右子树高度差，用 `-1` 作为 "不平衡" 的信号。

**【9. Time Complexity】** O(n)

---

### Function: `printTree()`

**【1. 做什么？】** 打印 BST 的 ASCII 可视化图形。

**【5. 谁调用？】** `FrontDeskController.printGuestTreeStructure()`, `printRoomTreeStructure()`

**【9. Time Complexity】** O(n)

---

## MyArrayList.java 的 Functions

---

### Function: `add(T newEntry)`

**【1. 做什么？】** 在列表末尾添加一个新元素。

**【6. 执行过程】**

Step 1 → 检查数组是否已满 (`numberOfEntries >= array.length`)  
Step 2 → 如果满了，调用 `doubleCapacity()` 扩容  
Step 3 → 把新元素放到 `array[numberOfEntries]`  
Step 4 → `numberOfEntries++`  

**【9. Time Complexity】** 摊销 O(1)（偶尔 O(n) 因为扩容复制）

---

### Function: `sort(Comparator<T> comparator)`

**【1. 做什么？】** 用 **Selection Sort** 对列表中的元素排序。

**【2. 为什么需要？】** 各种 Report 需要按不同标准排序。

**【3. Input】** `Comparator<T> comparator` — 定义排序规则的比较器

**【6. 执行过程】**

```
for i = 0 到 n-2:
    minOrMaxIdx = i
    for j = i+1 到 n-1:
        if array[j] < array[minOrMaxIdx]:
            minOrMaxIdx = j
    if minOrMaxIdx != i:
        交换 array[i] 和 array[minOrMaxIdx]
```

Step 1 → 外层循环从第 0 个到倒数第 2 个  
Step 2 → 内层循环在未排序区域找到最小元素  
Step 3 → 把最小元素与当前位置交换  

**【7. Data Structure】** Array

**【8. Algorithm】** Selection Sort（选择排序）

**【9. Time Complexity】** **O(n²)** — 两层嵌套循环

**【10. Space Complexity】** O(1) — 原地排序

**【11. 例子】**

排序 [300, 100, 200]:
```
Round 1: 找到最小 100，和 300 交换 → [100, 300, 200]
Round 2: 找到最小 200，和 300 交换 → [100, 200, 300]
```

**【12. 注意事项】** Selection Sort 效率不高（O(n²)），但对于作业中的小数据量是可以接受的。

---

### Function: `doubleCapacity()`

**【1. 做什么？】** 当数组满了时，创建一个两倍大的新数组，把旧数据复制过去。

**【9. Time Complexity】** O(n) — 需要复制所有元素

---

## ArrayQueue.java 的 Functions

---

### Function: `enqueue(T newEntry)`

**【1. 做什么？】** 在队列尾部添加一个新元素。

**【2. 为什么需要？】** 新的 Walk-In 客人需要加入等待队列。

**【6. 执行过程】**

Step 1 → 检查 null  
Step 2 → 如果数组满了，`doubleCapacity()`  
Step 3 → `rear = (rear + 1) % array.length`（环形移动）  
Step 4 → 把新元素放到 `array[rear]`  
Step 5 → `numberOfEntries++`  

**【7. Data Structure】** Circular Array Queue

**【9. Time Complexity】** 摊销 O(1)

**【11. 例子】**

Queue (front=0, rear=-1, 空):
```
enqueue(Sarah):  front=0, rear=0  → [Sarah, _, _, _]
enqueue(James):  front=0, rear=1  → [Sarah, James, _, _]
enqueue(Linda):  front=0, rear=2  → [Sarah, James, Linda, _]
```

---

### Function: `dequeue()`

**【1. 做什么？】** 移除并返回队列前端的元素。

**【2. 为什么需要？】** 处理下一位等待客人时，从队列前端取出。

**【6. 执行过程】**

Step 1 → 如果队列空，返回 null  
Step 2 → 取出 `array[front]` 保存  
Step 3 → 把 `array[front]` 设为 null（释放引用）  
Step 4 → `front = (front + 1) % array.length`（环形移动）  
Step 5 → `numberOfEntries--`  

**【9. Time Complexity】** O(1)

**【11. 例子】**

Queue: [Sarah, James, Linda] (front=0, rear=2)
```
dequeue() → 返回 Sarah, front=1 → [_, James, Linda]
dequeue() → 返回 James, front=2 → [_, _, Linda]
```

**【12. 注意事项】** `array[front] = null` 是为了避免内存泄漏 (stale reference)。

---

### Function: `toList()`

**【1. 做什么？】** 在不修改 Queue 的情况下，返回所有元素的快照列表。

**【2. 为什么需要？】** "View Waiting Queue" 需要显示所有等待的客人，但不能改变队列。

**【9. Time Complexity】** O(n)

---

## ArrayStack.java 的 Functions

---

### Function: `push(T newEntry)`

**【1. 做什么？】** 在栈顶压入一个新元素。

**【2. 为什么需要？】** 每次修改房间状态时，需要把变更记录压入栈中。

**【9. Time Complexity】** 摊销 O(1)

---

### Function: `pop()`

**【1. 做什么？】** 移除并返回栈顶元素。

**【2. 为什么需要？】** Rollback 时需要取出最近的变更记录来撤销。

**【6. 执行过程】**

Step 1 → 如果栈空，返回 null  
Step 2 → `numberOfEntries--`  
Step 3 → 取出 `array[numberOfEntries]`（就是栈顶）  
Step 4 → 把该位置设为 null  

**【9. Time Complexity】** O(1)

---

### Function: `peek()`

**【1. 做什么？】** 查看栈顶元素但不移除。

**【9. Time Complexity】** O(1)

---

### Function: `toList()`

**【1. 做什么？】** 返回从栈顶到栈底的所有元素（最新的在前面）。

**【9. Time Complexity】** O(n)

---

## BookingController.java 的 Functions

---

### Function: `registerWalkInGuest(String name, String icNo, String tier, int points)`

**【1. 做什么？】** 注册一个新的 Walk-In 客人：生成确认号、创建 Guest、放入等待队列、同步到 Master BST。

**【2. 为什么需要？】** 当有客人到柜台要求预订时。

**【3. Input】** 客人名字、IC 号、忠诚度等级、积分

**【4. Output】** `Guest` — 新创建的 Guest 对象

**【5. 谁调用？】** `BookingUI.handleRegisterWalkIn()`

**【6. 执行过程】**

Step 1 → 生成新的 confirmationNumber（自动递增，如 20000004）  
Step 2 → 创建新 Guest 对象  
Step 3 → `waitingQueue.enqueue(newGuest)` — 加入等待队列  
Step 4 → `registeredGuests.add(newGuest)` — 加入已登记列表  
Step 5 → `masterGuestRegistry.add(newGuest)` — 同步到共享 BST  

**【7. Data Structure】** Queue (enqueue)、MyArrayList (add)、BST (add)

**【8. Algorithm】** BST Insertion（同步时）

**【9. Time Complexity】** O(log n) — BST 插入

**【11. 例子】**

当前 Queue: [Sarah, James, Linda]
调用 `registerWalkInGuest("Tom", "030101-14-1111", "Standard", 0)`
→ 生成 confirmationNumber = "20000004"
→ Queue 变成: [Sarah, James, Linda, Tom]
→ Tom 也被加入 BST

---

### Function: `findGuestByIC(String icNo)`

**【1. 做什么？】** 根据 IC 号在 Master BST 中查找已有的客人。

**【2. 为什么需要？】** 注册 Walk-In 时检查这个客人是否已经是会员。

**【3. Input】** IC / Passport 号码

**【4. Output】** `Guest` 或 `null`

**【6. 执行过程】**

Step 1 → 清理输入（去掉特殊字符，转小写）  
Step 2 → `inOrderTraversal()` 取出 BST 所有 Guest  
Step 3 → 逐个比较 IC 号（也清理后比较）  

**【8. Algorithm】** Linear Search（因为 IC 不是 BST 的 Key）

**【9. Time Complexity】** O(n) — 必须遍历所有节点

**【12. 注意事项】** IC 号不是 BST 的排序 Key（Key 是 confirmationNumber），所以**不能用 BST Search**，只能线性遍历。

---

### Function: `processNextGuest(String roomNumber, String checkInDate, int numberOfNights)`

**【1. 做什么？】** 处理队列中的下一位客人：从队列取出、验证房间、创建预订、更新房间状态。

**【2. 为什么需要？】** 这是 Booking 模块的核心操作——把等待客人和可用房间配对。

**【3. Input】** 房间号、入住日期、住几晚

**【4. Output】** `int` — 1=成功, -1=队列空, -2=房间不存在, -3=房间不可用

**【5. 谁调用？】** `BookingUI.handleProcessNextGuest()`

**【6. 执行过程】**

Step 1 → 检查队列是否空 → 空则返回 -1  
Step 2 → 用 `findRoomByNumber()` 找房间 → 找不到返回 -2  
Step 3 → 检查房间是否 "Ready for Check-In" → 否则返回 -3  
Step 4 → `waitingQueue.dequeue()` 取出队列前端的客人  
Step 5 → 生成 Booking ID (如 "BK0002")  
Step 6 → 创建 `new Booking(...)` 并加入 bookingList  
Step 7 → 把房间状态改为 "Reserved"  
Step 8 → 更新 Guest 的 bookingStatus、assignedRoomNumber、roomType 等  

**【7. Data Structure】** Queue (dequeue)、MyArrayList (add、linear search)

**【8. Algorithm】** Linear Search（找房间）

**【9. Time Complexity】** O(R) — R = 房间数量（因为 findRoomByNumber 线性搜索）

**【11. 例子】**

Queue: [Sarah(20000001), James(20000002), Linda(20000003)]
调用 `processNextGuest("101", "2026-08-14", 2)`

→ dequeue() 取出 Sarah
→ 创建 Booking: BK0002, Sarah, Room 101, 2 nights
→ Room 101 状态: "Ready for Check-In" → "Reserved"
→ Sarah 状态: bookingStatus="Reserved", assignedRoom="101"
→ Queue: [James, Linda]
→ 返回 1 (成功)

---

### Function: `cancelBooking(String bookingId)`

**【1. 做什么？】** 取消一笔预订。

**【4. Output】** `int` — 1=成功, -1=找不到, -2=已取消, -3=已入住无法取消, -4=已退房无法取消

**【6. 执行过程】**

Step 1 → 遍历 bookingList 找到对应的 Booking  
Step 2 → 检查 Booking 是否已经 Cancelled → 是则返回 -2  
Step 3 → 在 Master BST 中找到对应 Guest → 检查是否已入住/已退房  
Step 4 → 把 Guest 的 bookingStatus 改为 "Cancelled"，清空房间信息  
Step 5 → 把 Booking 的 status 改为 "Cancelled"  
Step 6 → 如果房间状态是 "Reserved"，改回 "Ready for Check-In"  

**【9. Time Complexity】** O(B + log G) — B=预订数（线性搜索）+ log G（BST 搜索 Guest）

---

### Function: `getFilteredAndSortedBookings(String roomTypeFilter, int minNights, boolean ascending)`

**【1. 做什么？】** 根据房间类型和最低住宿夜数筛选预订，再按总价排序。

**【2. 为什么需要？】** Report 1: Booking Summary。

**【6. 执行过程】**

Step 1 → 遍历 bookingList，筛选出符合条件的 Booking  
Step 2 → 用 `filtered.sort(Comparator)` 按 totalPrice 排序（Selection Sort）  

**【8. Algorithm】** Linear Search（筛选）+ Selection Sort（排序）

**【9. Time Complexity】** O(B) 筛选 + O(k²) 排序，k = 筛选后的数量

---

### Function: `getFilteredAndSortedGuests(String tierFilter, String statusFilter, boolean sortAscending)`

**【1. 做什么？】** 根据忠诚度等级和状态（Waiting/Processed）筛选客人，按确认号排序。

**【2. 为什么需要？】** Report 2: Guest Registration & Tier Analysis。

**【6. 执行过程】**

Step 1 → `waitingQueue.toList()` 获取当前等待队列的快照  
Step 2 → 遍历 registeredGuests，判断每个人是否在等待队列中  
Step 3 → 筛选符合 tier 和 status 条件的 Guest  
Step 4 → 用 Selection Sort 按 confirmationNumber 排序  

**【9. Time Complexity】** O(R × Q) 筛选 + O(k²) 排序

---

### Function: `getRegistrationSummary()`

**【1. 做什么？】** 返回注册统计数据：总人数、等待人数、已处理人数、各等级人数。

**【4. Output】** `int[]` — 长度 7 的数组

**【9. Time Complexity】** O(R)

---

## FrontDeskController.java 的 Functions

---

### Function: `searchGuestByConfirmationNumber(String confirmNo)`

**【1. 做什么？】** 用确认号在 BST 中搜索客人。

**【2. 为什么需要？】** 这是最常用的搜索方式——确认号是 BST 的 Key。

**【6. 执行过程】**

Step 1 → 创建一个 "dummy" Guest 对象，只设置 confirmationNumber  
Step 2 → 调用 `guestTree.search(dummy)` — BST 用 `compareTo()` 比较 confirmationNumber  

**【8. Algorithm】** BST Search — 利用 BST 的排序特性进行二分查找

**【9. Time Complexity】** **O(log n)** 平均 — 这就是使用 BST 的核心优势！

**【11. 例子】**

搜索 "10000003":
```
创建 dummy Guest: confirmationNumber = "10000003"
调用 guestTree.search(dummy)
→ BST 按 confirmationNumber 的字典序比较
→ 最终找到 Charlie Lim
```

**【12. 注意事项】** 这里的 "dummy" 对象只是一个搜索用的空壳，它的 guestName 和其他字段是空的，只有 confirmationNumber 有值。BST 只看 `compareTo()` 的结果（即 confirmationNumber）。

---

### Function: `searchGuestsByName(String nameQuery)`

**【1. 做什么？】** 根据客人名字（部分匹配）搜索。

**【6. 执行过程】**

Step 1 → `inOrderTraversal()` 获取所有 Guest  
Step 2 → 逐个检查 `guestName.contains(queryLower)`  

**【8. Algorithm】** In-Order Traversal + Linear Search with substring matching

**【9. Time Complexity】** O(n) — 名字不是 BST 的 Key，必须遍历所有

---

### Function: `searchGuestByIC(String icNo)`

**【1. 做什么？】** 根据 IC 号搜索客人（去掉特殊字符后比较）。

**【9. Time Complexity】** O(n) — IC 也不是 BST 的 Key

---

### Function: `registerGuest(Guest guest)`

**【1. 做什么？】** 注册新客人到 BST。

**【6. 执行过程】**

Step 1 → 检查 guest 和 confirmationNumber 是否为 null  
Step 2 → 检查 BST 中是否已存在 (`guestTree.contains()`) → 存在则返回 false  
Step 3 → `guestTree.add(guest)` 插入 BST  

**【9. Time Complexity】** O(log n)

---

### Function: `removeGuest(String confirmNo)`

**【1. 做什么？】** 从 BST 中移除客人。

**【6. 执行过程】**

Step 1 → 创建 dummy Guest  
Step 2 → `guestTree.remove(dummy)` — BST Deletion  

**【9. Time Complexity】** O(log n)

---

### Function: `processCheckIn(String confirmationNumber, String roomNumber, double baseRoomPrice)`

**【1. 做什么？】** 处理客人入住：验证客人和房间状态，设置房间为 Occupied，更新客人状态为 CheckedIn。

**【4. Output】** `int` — 1=成功, -1=客人不存在, -2=房间不存在, -3=房间不可用, -4=已入住, -5=房间被他人预订, -6=已退房, -7=已取消

**【5. 谁调用？】** `FrontDeskUI.handleCheckIn()`

**【6. 执行过程】**

Step 1 → `syncRoomTree()` 同步 Room BST  
Step 2 → BST 搜索 Guest  
Step 3 → 检查 Guest 是否已入住 (`isCheckedIn()`) → 已入住返回 -4  
Step 4 → 检查 bookingStatus 是否为 CheckedOut / Cancelled → 对应返回 -6 / -7  
Step 5 → 检查 `activeCheckedInConfirmations` 列表（Double Lock 双重检查）  
Step 6 → BST 搜索 Room  
Step 7 → 检查房间状态是否为 "Ready for Check-In" 或 "Reserved"  
Step 8 → 如果是 "Reserved"，验证是否是这个客人的预订  
Step 9 → 设置房间状态为 "Occupied"  
Step 10 → 设置客人状态为 "CheckedIn"  
Step 11 → 把确认号加入 `activeCheckedInConfirmations`  

**【7. Data Structure】** BST (search)、MyArrayList (linear search activeCheckedInConfirmations)

**【9. Time Complexity】** O(log G + log R + C) — G=客人数、R=房间数、C=已入住数

**【11. 例子】**

入住 Bob(10000002) 到 Room 103:
```
1. 搜索 BST 找到 Bob → bookingStatus = "Reserved"
2. Room 103 状态 = "Ready for Check-In" ✓
3. Room 103 → "Occupied"
4. Bob → bookingStatus = "CheckedIn", assignedRoom = "103"
5. activeCheckedInConfirmations 增加 "10000002"
→ 返回 1
```

---

### Function: `processCheckOut(String confirmationNumber)`

**【1. 做什么？】** 处理客人退房：房间标为 Dirty，客人标为 CheckedOut。

**【4. Output】** `int` — 1=成功, -1=客人不存在, -2=客人未入住

**【6. 执行过程】**

Step 1 → BST 搜索 Guest  
Step 2 → 检查 Guest 是否已入住  
Step 3 → 找到 Guest 的房间 → 设为 "Dirty"  
Step 4 → Guest bookingStatus 改为 "CheckedOut"  
Step 5 → 从 `activeCheckedInConfirmations` 中移除该确认号  

**【12. 注意事项】** 退房时故意保留 Guest 的 `assignedRoomNumber`、`roomType`、`roomRate`，这是为了报告和 Loyalty 模块可以查看历史记录。

---

### Function: `processRoomTransfer(String confirmationNumber, String newRoomNumber)`

**【1. 做什么？】** 换房：旧房间标为 Dirty，新房间标为 Occupied。

**【4. Output】** `int` — 1=成功, -1=客人不存在, -2=未入住, -3=新房间不存在, -4=新房间不可用, -5=选择了同一间房

**【6. 执行过程】**

Step 1 → 搜索 Guest → 检查是否已入住  
Step 2 → 检查新旧房间是否相同  
Step 3 → 搜索新房间 → 检查是否 "Ready for Check-In"  
Step 4 → 旧房间 → "Dirty"  
Step 5 → 新房间 → "Occupied"  
Step 6 → 更新 Guest 的房间信息（但**不改变 effectiveRoomRate**，保留原价作为升级福利）  

---

### Function: `suggestRoomUpgrade(String currentRoomNo)`

**【1. 做什么？】** 为高等级会员（Platinum / Gold）推荐免费升级的房间。

**【2. 为什么需要？】** Loyalty 福利——高等级会员可以免费升级到更好的房间，但只按原价收费。

**【6. 执行过程】**

Step 1 → 找到当前房间  
Step 2 → 遍历所有房间，找到 "Ready for Check-In" 且价格更高的房间  
Step 3 → 在候选中选**最便宜的那个**（最小的可升级房间）  

**【9. Time Complexity】** O(R)

**【11. 例子】**

当前房间: Room 103 (Standard Room, RM180)  
所有可用房间:
- Room 101 (Deluxe Suite, RM350, Ready) ← 候选
- Room 202 (Deluxe Suite, RM400, Ready) ← 候选
- Room 201 (Presidential Suite, RM950, Ready) ← 候选

推荐: Room 101 (RM350) — 价格比 RM180 高，且是候选中最便宜的

---

### Function: `getDiscountPercentage(String loyaltyTier)`

**【1. 做什么？】** 根据会员等级返回折扣百分比。

**【4. Output】** `double` — Platinum=20%, Gold=10%, Silver=5%, Standard=0%

**【9. Time Complexity】** O(1)

---

### Function: `syncRoomTree()`

**【1. 做什么？】** 把 sharedRoomList 的数据同步到 Room BST。

**【2. 为什么需要？】** 因为其他模块（Booking、Housekeeping）可能修改了 Room List 中的数据，FrontDesk 需要同步这些变化到自己的 Room BST。

**【6. 执行过程】**

Step 1 → 清空 roomTree  
Step 2 → 遍历 sharedRoomList，把每个 Room 重新加入 roomTree  

**【9. Time Complexity】** O(R log R)

---

### Function: `getRoomStatusSummary()`

**【1. 做什么？】** 统计各种状态的房间数量。

**【4. Output】** `int[]` — [0]=总数, [1]=Ready, [2]=Occupied, [3]=Dirty, [4]=Cleaning, [5]=Reserved

---

### Function: `getFilteredAndSortedRooms(String statusFilter, double maxPrice, boolean sortAsc)`

**【1. 做什么？】** 按状态和最高价筛选房间，然后按价格排序。

**【8. Algorithm】** Linear Search（筛选）+ Selection Sort（排序）

---

### Function: `getFilteredAndSortedGuests(String tierFilter, int minPoints)`

**【1. 做什么？】** 按等级和最低积分筛选客人，按积分降序排序。

**【8. Algorithm】** In-Order Traversal + Linear Search + Selection Sort

---

### Function: `getGuestTreeDiagnostics()`

**【1. 做什么？】** 返回 BST 的诊断信息（节点数、高度、叶子数、是否平衡、最小/最大 Key）。

**【4. Output】** `String[]` — 6 个诊断值

---

## HousekeepingController.java 的 Functions

---

### Function: `advanceRoomStatus(String roomNumber, String staffName)`

**【1. 做什么？】** 将房间推进到清洁流程的下一阶段。

**【2. 为什么需要？】** 清洁流程有固定顺序：`Dirty → Cleaning In Progress → Inspected → Ready for Check-In`

**【4. Output】** `int` — 1=成功, -1=房间不存在, -2=已经是最终阶段, -3=不在清洁流程中（如 Occupied）

**【5. 谁调用？】** `HousekeepingUI.advanceRoomStatus()`

**【6. 执行过程】**

Step 1 → `findRoomByNumber()` 找到房间  
Step 2 → `getStatusIndex()` 找到当前状态在 STATUS_SEQUENCE 中的位置  
Step 3 → 如果 index = -1（不在序列中，如 "Occupied"）→ 返回 -3  
Step 4 → 如果 index = 3（已是最后阶段）→ 返回 -2  
Step 5 → 设置新状态为 STATUS_SEQUENCE[index + 1]  
Step 6 → 创建 HousekeepingLog 并 `taskLogStack.push()` 压入栈  

**【7. Data Structure】** MyArrayList (linear search) + ArrayStack (push)

**【9. Time Complexity】** O(R) — 主要在 findRoomByNumber 的线性搜索

**【11. 例子】**

Room 101, 当前状态 "Dirty":
```
getStatusIndex("Dirty") = 0
0 < 3, 可以推进
新状态 = STATUS_SEQUENCE[1] = "Cleaning In Progress"
Room 101 → "Cleaning In Progress"
Push log: "101 | Dirty -> Cleaning In Progress | By: Ali | 2026-08-14"
```

---

### Function: `setRoomStatus(String roomNumber, String newStatus, String staffName)`

**【1. 做什么？】** 手动设置房间的清洁状态（可以跳过流程或回退）。

**【2. 为什么需要？】** 有时需要直接修正状态（比如客人临时延迟退房，房间需要从 "Ready" 改回 "Dirty"）。

**【4. Output】** `int` — 1=成功, -1=房间不存在, -2=无效状态名

**【9. Time Complexity】** O(R)

---

### Function: `rollbackLastChange()`

**【1. 做什么？】** 撤销最近一次状态变更。

**【2. 为什么需要？】** 如果操作员不小心把状态改错了，可以立刻撤销。

**【6. 执行过程】**

Step 1 → `taskLogStack.pop()` 弹出最近的 Log  
Step 2 → 找到对应的房间  
Step 3 → 把房间状态恢复为 `lastLog.getPreviousStatus()`  

**【7. Data Structure】** Stack (pop) — LIFO 特性让最近的操作最先被撤销

**【9. Time Complexity】** O(R)

**【11. 例子】**

Stack top: "Room 101 | Dirty -> Cleaning In Progress | By: Ali"
```
pop() → 取出这条记录
Room 101 当前是 "Cleaning In Progress"
恢复为 previousStatus = "Dirty"
→ Room 101 回到 "Dirty"
```

---

### Function: `getFilteredTaskLog(String roomFilter, String statusFilter, boolean newestFirst)`

**【1. 做什么？】** 筛选并排序清洁任务日志。

**【2. 为什么需要？】** Report: 可以按房间号、状态筛选日志，并按时间排序。

**【6. 执行过程】**

Step 1 → `taskLogStack.toList()` 获取所有日志（不修改 Stack）  
Step 2 → 逐条检查 roomNumber 和 newStatus 是否符合筛选条件  
Step 3 → Selection Sort 按 taskId 排序  

**【8. Algorithm】** Linear Search + Selection Sort

---

### Function: `getRoomsNeedingAttention()`

**【1. 做什么？】** 找出所有还没到 "Ready for Check-In" 的房间，并按紧急程度排序。

**【6. 执行过程】**

Step 1 → 遍历 roomList，找出 statusIndex < 3（不是 "Ready for Check-In"）的房间  
Step 2 → 按 statusIndex 排序（statusIndex 越小越紧急，越需要关注）  

**【11. 例子】**
```
Room 101: Dirty (index=0) ← 最紧急
Room 102: Cleaning In Progress (index=1) ← 次紧急
Room 103: Inspected (index=2) ← 接近完成
```

---

## FrontDeskUI.java 的重要 Functions

---

### Function: `handleCheckIn()`

**【1. 做什么？】** 处理客人入住的完整 UI 流程。

**【6. 执行过程】**

Step 1 → 读取确认号（8 位数字）  
Step 2 → BST 搜索 Guest → 没找到则报错  
Step 3 → 检查是否已入住 / 已退房 / 已取消  
Step 4 → 如果 Guest 已有 assignedRoomNumber → 自动检测到预订房间  
Step 5 → 如果没有 → 显示可用房间列表，让用户选  
Step 6 → 如果是 Platinum / Gold 会员 → `suggestRoomUpgrade()` 推荐升级  
Step 7 → 调用 `controller.processCheckIn()` 执行入住  
Step 8 → 成功后询问入住日期、住几晚、特殊要求  

这是一个**很好的 Business Logic 例子**：检查多种状态、推荐升级、然后处理入住。

---

### Function: `handleBillingReceipt()`

**【1. 做什么？】** 生成账单并可选择退房。

**【6. 执行过程】**

Step 1 → BST 搜索 Guest  
Step 2 → 检查是否已入住  
Step 3 → 计算账单：`nightRate × nights - discount`  
Step 4 → 显示发票  
Step 5 → 询问是否退房  
Step 6 → 如果退房：
  - 计算积分 (subtotal / 10)
  - 更新忠诚度等级（≥1000 Platinum, ≥500 Gold, ≥200 Silver）
  - 调用 `processCheckOut()`  

---

### Function: `handleRoomTransfer()`

**【1. 做什么？】** 处理换房。

**【6. 执行过程】**

Step 1 → BST 搜索 Guest → 检查是否已入住  
Step 2 → 显示当前房间信息  
Step 3 → 显示所有可用房间  
Step 4 → 用户选择新房间  
Step 5 → 调用 `processRoomTransfer()`  

---

### Function: `readValidConfirmationNumber(String prompt)`

**【1. 做什么？】** 反复提示用户输入，直到输入合法的 8 位数字。

**【6. 执行过程】**

Step 1 → 显示提示  
Step 2 → 读取输入  
Step 3 → 用正则表达式 `\\d{8}` 检查是否是 8 位数字  
Step 4 → 不符合则重新提示  

**【9. Time Complexity】** O(1) per attempt

---

# 第四部分：重要代码段解释

---

## Guest.java 中的 `compareTo()`

```java
public int compareTo(Guest other) {
    if (other == null || other.confirmationNumber == null) return 1;
    if (this.confirmationNumber == null) return -1;
    return this.confirmationNumber.compareToIgnoreCase(other.confirmationNumber);
}
```

**为什么存在？**
→ BST 需要知道怎么比较两个 Guest。这个方法定义了 **用 confirmationNumber 的字典序** 来排列。

**如果删除这一行？**
→ BST 的 `add()`, `search()`, `remove()` 全部无法工作，因为它们依赖 `compareTo()` 来决定往左还是往右走。

---

## ArrayQueue.java 中的 `% array.length`

```java
rear = (rear + 1) % array.length;
```

**为什么存在？**
→ 这是 **Circular Array（环形数组）** 的核心。当 `rear` 到达数组末尾时，`% array.length` 让它回到数组开头，实现循环利用空间。

**如果删除 `% array.length`？**
→ 当 rear 超过数组长度时会 `ArrayIndexOutOfBoundsException`。

---

## FrontDeskController.java 中的 Dummy Guest

```java
Guest targetDummy = new Guest("", confirmNo.trim(), "", 0);
return guestTree.search(targetDummy);
```

**为什么存在？**
→ BST 的 `search()` 需要一个 Guest 对象来调用 `compareTo()`。但我们只知道 confirmationNumber，所以创建一个"空壳" Guest，只填入 confirmationNumber，用它来搜索。

**如果删除？**
→ 无法使用 BST 搜索功能。

---

## HousekeepingController.java 中的 STATUS_SEQUENCE

```java
private static final String[] STATUS_SEQUENCE = {
    "Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In"
};
```

**为什么存在？**
→ 定义了清洁流程的固定顺序。`advanceRoomStatus()` 使用数组索引来确定"下一阶段"是什么。

---

## FrontDeskUI.java 中的 Loyalty Points 计算

```java
int earnedPoints = (int) (subtotal / 10.0);
```

**为什么存在？**
→ 每消费 RM10 赚 1 积分。使用 subtotal（折扣前金额）计算，这是行业标准做法。

```java
if (updatedPoints >= 1000) guest.setLoyaltyTier("Platinum");
else if (updatedPoints >= 500) guest.setLoyaltyTier("Gold");
else if (updatedPoints >= 200) guest.setLoyaltyTier("Silver");
else guest.setLoyaltyTier("Standard");
```

**为什么存在？**
→ 自动升级/降级忠诚度等级。积分到达门槛自动升级。

---

# 第五部分：Function 之间的调用关系

---

## 模块 1: Booking 流程

```
App.main()
 ↓
BookingUI.displayMenu()
 ├── handleRegisterWalkIn()
 │    ├── BookingController.findGuestByIC()
 │    │    └── BST.inOrderTraversal() + Linear Search
 │    └── BookingController.registerWalkInGuest()
 │         ├── ArrayQueue.enqueue()
 │         ├── MyArrayList.add()
 │         └── BST.add()
 │
 ├── handleViewWaitingQueue()
 │    └── BookingController.getWaitingQueueList()
 │         └── ArrayQueue.toList()
 │
 ├── handleProcessNextGuest()
 │    ├── BookingController.peekNextGuest()
 │    │    └── ArrayQueue.getFront()
 │    ├── BookingController.getAvailableRooms()
 │    │    └── MyArrayList linear scan
 │    ├── BookingController.findRoomByNumber()
 │    │    └── MyArrayList linear search
 │    └── BookingController.processNextGuest()
 │         ├── ArrayQueue.dequeue()
 │         └── MyArrayList.add() [新 Booking]
 │
 ├── handleCancelBooking()
 │    └── BookingController.cancelBooking()
 │         ├── MyArrayList linear search [找 Booking]
 │         ├── BST.search() [找 Guest]
 │         └── findRoomByNumber() [释放房间]
 │
 ├── displayReport1()
 │    └── BookingController.getFilteredAndSortedBookings()
 │         ├── Linear Search [筛选]
 │         └── MyArrayList.sort() [Selection Sort]
 │
 └── displayReport2()
      ├── BookingController.getFilteredAndSortedGuests()
      │    ├── ArrayQueue.toList() [快照]
      │    ├── Linear Search [筛选]
      │    └── MyArrayList.sort() [Selection Sort]
      └── BookingController.getRegistrationSummary()
```

---

## 模块 2: Housekeeping 流程

```
App.main()
 ↓
HousekeepingUI.displayMenu()
 ├── viewAllRooms()
 │    └── HousekeepingController.getAllRooms()
 │
 ├── advanceRoomStatus()
 │    └── HousekeepingController.advanceRoomStatus()
 │         ├── findRoomByNumber() [Linear Search]
 │         ├── getStatusIndex() [Array lookup]
 │         └── ArrayStack.push() [记录日志]
 │
 ├── manualSetStatus()
 │    └── HousekeepingController.setRoomStatus()
 │         ├── findRoomByNumber()
 │         └── ArrayStack.push()
 │
 ├── rollbackChange()
 │    ├── HousekeepingController.peekLastChange()
 │    │    └── ArrayStack.peek()
 │    └── HousekeepingController.rollbackLastChange()
 │         ├── ArrayStack.pop()
 │         └── findRoomByNumber() [恢复状态]
 │
 ├── roomStatusSummaryReport()
 │    └── HousekeepingController.getRoomStatusSummary()
 │
 ├── filteredTaskLogReport()
 │    └── HousekeepingController.getFilteredTaskLog()
 │         ├── ArrayStack.toList()
 │         ├── Linear Search [筛选]
 │         └── MyArrayList.sort() [Selection Sort]
 │
 └── roomsNeedingAttentionReport()
      └── HousekeepingController.getRoomsNeedingAttention()
           ├── Linear Search [筛选]
           └── MyArrayList.sort() [Selection Sort]
```

---

## 模块 3: Front Desk 流程

```
App.main()
 ↓
FrontDeskUI.displayMenu()
 ├── handleGuestSearch()
 │    ├── [By ConfirmNo] → FrontDeskController.searchGuestByConfirmationNumber()
 │    │                     └── BST.search() ← O(log n) ★
 │    ├── [By Name]      → FrontDeskController.searchGuestsByName()
 │    │                     └── BST.inOrderTraversal() + Linear Search
 │    ├── [By Range]     → FrontDeskController.searchGuestsByConfirmationRange()
 │    │                     └── BST.rangeSearch() ← O(log n + k) ★
 │    └── [By IC]        → FrontDeskController.searchGuestByIC()
 │                          └── BST.inOrderTraversal() + Linear Search
 │
 ├── handleGuestManagement()
 │    ├── [Register] → FrontDeskController.registerGuest()
 │    │                 └── BST.contains() + BST.add()
 │    └── [Remove]   → FrontDeskController.removeGuest()
 │                      └── BST.remove()
 │
 ├── handleCheckIn()
 │    ├── FrontDeskController.searchGuestByConfirmationNumber() [BST Search]
 │    ├── FrontDeskController.suggestRoomUpgrade() [遍历 Room BST]
 │    └── FrontDeskController.processCheckIn()
 │         ├── BST.search() [Guest]
 │         ├── BST.search() [Room]
 │         └── 更新状态
 │
 ├── handleRoomTransfer()
 │    └── FrontDeskController.processRoomTransfer()
 │
 ├── handleBillingReceipt()
 │    ├── FrontDeskController.searchGuestByConfirmationNumber()
 │    ├── FrontDeskController.getDiscountPercentage()
 │    └── FrontDeskController.processCheckOut()
 │
 ├── displayReportsSubmenu()
 │    ├── Report 1: getRoomStatusSummary() + calculateOccupancyRate() + calculateEstimatedDailyRevenue()
 │    ├── Report 2: getFilteredAndSortedRooms()
 │    └── Report 3: getFilteredAndSortedGuests()
 │
 └── displayTreeDiagnostics()
      ├── getGuestTreeDiagnostics()
      ├── printGuestTreeStructure() → BST.printTree()
      ├── printRoomTreeStructure() → BST.printTree()
      ├── getGuestTraversal() → BST.inOrder/preOrder/postOrder
      └── rebalanceTrees() → BST.rebalance()
```

---

# 第六部分：DSA 分析

---

## 使用了哪些 Data Structures？

| Data Structure | 具体实现 | 用在哪里 | 为什么使用 |
|---------------|---------|---------|-----------|
| **Binary Search Tree (BST)** | `BinarySearchTree<T>` | Master Guest Registry; Room Tree (FrontDesk) | 支持 O(log n) 搜索/插入/删除；支持范围搜索；支持有序遍历 |
| **Circular Array Queue** | `ArrayQueue<T>` | BookingController.waitingQueue | 客人先来先服务 (FIFO)；O(1) enqueue/dequeue |
| **Array Stack** | `ArrayStack<T>` | HousekeepingController.taskLogStack | 最后的操作最先撤销 (LIFO)；O(1) push/pop |
| **Dynamic Array List** | `MyArrayList<T>` | 共享房间列表；预订记录；各种筛选结果 | 通用的有序集合；支持按索引访问；支持排序 |

## 使用了哪些 Algorithms？

| Algorithm | 用在哪里 | 为什么 | Complexity |
|-----------|---------|--------|------------|
| **BST Search** | 按 confirmationNumber 搜索 Guest | 利用排序特性实现高效搜索 | O(log n) |
| **BST Insertion** | 注册新 Guest | 维持排序顺序 | O(log n) |
| **BST Deletion** | 删除 Guest | 处理叶子/单子/双子三种情况 | O(log n) |
| **BST Range Search** | 按范围搜索 Guest | 剪枝优化的遍历 | O(log n + k) |
| **BST Traversals** | In/Pre/Post-Order | 获取有序列表 / 分析结构 | O(n) |
| **BST Rebalance** | 重建平衡树 | 防止退化为链表 | O(n) |
| **Selection Sort** | 排序筛选结果 | 简单且满足作业要求 | O(n²) |
| **Linear Search** | 按 IC/Name 搜索；找房间 | 这些字段不是 BST 的 Key | O(n) |
| **FIFO Queue** | 客人排队等待 | 保证公平的先到先得顺序 | enqueue/dequeue O(1) |
| **LIFO Stack** | 操作日志回滚 | 最近的操作最先被撤销 | push/pop O(1) |

## 每个 Data Structure 解决了什么问题？

### BST 解决的问题
→ **快速按 confirmationNumber 搜索 Guest**。如果用 ArrayList，每次搜索都要从头到尾遍历 O(n)。用 BST，每次比较可以排除一半数据 O(log n)。
→ **范围搜索**。BST 可以高效地找出在某个范围内的所有 Guest。

### Queue 解决的问题
→ **维护客人的等待顺序**。先来的客人必须先被服务，Queue 的 FIFO 特性天然保证这一点。

### Stack 解决的问题
→ **实现撤销功能**。如果清洁人员不小心把 Room 101 从 "Dirty" 改成 "Inspected"（跳过了中间步骤），可以用 Stack 的 pop 立刻撤回到 "Dirty"。

### MyArrayList 解决的问题
→ **通用存储和排序**。需要存一组数据并按不同标准排序（价格/积分/确认号等）时使用。

---

# 第七部分：Business Logic（业务规则）

---

## Booking 模块的规则

| 规则 | 代码位置 |
|------|---------|
| 客人必须先进入 Queue，才能被处理（分配房间） | `registerWalkInGuest()` → `enqueue()` |
| 处理顺序必须是 FIFO（先来先服务） | `processNextGuest()` → `dequeue()` |
| 只能分配 "Ready for Check-In" 状态的房间 | `processNextGuest()` 检查 roomStatus |
| 房间被分配后状态变为 "Reserved" | `processNextGuest()` 设置 roomStatus |
| 不能取消已入住的预订 | `cancelBooking()` 检查 `isCheckedIn()` 返回 -3 |
| 不能取消已退房的预订 | `cancelBooking()` 检查 bookingStatus 返回 -4 |
| 取消预订会释放房间回 "Ready for Check-In" | `cancelBooking()` 恢复 roomStatus |

## Front Desk 模块的规则

| 规则 | 代码位置 |
|------|---------|
| 同一个客人不能同时入住两间房 | `processCheckIn()` 检查 `isCheckedIn()` 返回 -4 |
| 已退房的确认号不能再次入住 | `processCheckIn()` 检查 "CheckedOut" 返回 -6 |
| 已取消的确认号不能入住 | `processCheckIn()` 检查 "Cancelled" 返回 -7 |
| 只能入住 "Ready for Check-In" 或 "Reserved" 的房间 | `processCheckIn()` 检查 roomStatus |
| "Reserved" 的房间只能给预订的那个客人 | `processCheckIn()` 比较 assignedRoomNumber 返回 -5 |
| 不能删除正在入住的客人 | `handleGuestManagement()` 检查 `isCheckedIn()` |
| 不能给未入住的客人生成账单 | `handleBillingReceipt()` 检查 `isCheckedIn()` |
| Platinum/Gold 会员可以获得免费房间升级 | `handleCheckIn()` 调用 `suggestRoomUpgrade()` |
| 升级后按原价收费 | `processCheckIn()` 使用传入的 baseRoomPrice |
| 退房后房间变为 "Dirty" | `processCheckOut()` 设置 roomStatus |
| 换房后旧房间变 "Dirty"，新房间变 "Occupied" | `processRoomTransfer()` |
| RM10 = 1 积分 | `handleBillingReceipt()` → `subtotal / 10` |
| ≥1000 分 = Platinum, ≥500 = Gold, ≥200 = Silver | `handleBillingReceipt()` 自动升级 |
| Platinum 折扣 20%, Gold 10%, Silver 5% | `getDiscountPercentage()` |

## Housekeeping 模块的规则

| 规则 | 代码位置 |
|------|---------|
| 清洁流程必须按顺序: Dirty → Cleaning → Inspected → Ready | `advanceRoomStatus()` 使用 STATUS_SEQUENCE |
| 已经是 "Ready for Check-In" 的房间不能再推进 | `advanceRoomStatus()` 返回 -2 |
| "Occupied" 等非清洁流程中的房间不能推进 | `advanceRoomStatus()` 返回 -3 |
| 每次状态变更都会记录到 Stack | `taskLogStack.push()` |
| 只能撤销最近一次操作 | `rollbackLastChange()` → `pop()` |

## 跨模块的规则

| 规则 | 说明 |
|------|------|
| **所有模块共享同一份 Guest 和 Room 数据** | App 创建共享引用，传给所有 UI/Controller |
| Booking 预订房间后 → Front Desk 可以看到 "Reserved" 状态 | 因为操作的是同一个 Room 对象 |
| Front Desk 退房后 → Housekeeping 看到 "Dirty" | 同上 |
| Housekeeping 清洁完 → Booking/FrontDesk 看到 "Ready for Check-In" | 同上 |
| Booking 注册的新客人 → Front Desk 可以搜索到 | 新客人同步到 masterGuestRegistry BST |

---

# 第八部分：完整地图 & Cheat Sheet

---

## Class → 用途速查

| Class | 用途 |
|-------|------|
| `App` | 程序入口，初始化共享数据，显示主菜单 |
| `Guest` | 客人实体，储存身份/住宿/忠诚度信息 |
| `Room` | 房间实体，储存房号/类型/状态/价格 |
| `Booking` | 预订记录实体 |
| `HousekeepingLog` | 清洁状态变更记录 |
| `BinarySearchTree` | BST 实现，管理 Guest/Room |
| `MyArrayList` | 动态数组列表，通用存储+排序 |
| `ArrayQueue` | 环形数组队列，客人排队 |
| `ArrayStack` | 数组栈，清洁日志+撤销 |
| `BookingUI` | Booking 模块界面 |
| `BookingController` | Booking 业务逻辑 |
| `FrontDeskUI` | Front Desk 界面 |
| `FrontDeskController` | Front Desk 业务逻辑 |
| `HousekeepingUI` | Housekeeping 界面 |
| `HousekeepingController` | Housekeeping 业务逻辑 |
| `LoyaltyUI` | Loyalty 界面 (Placeholder) |
| `LoyaltyController` | Loyalty 逻辑 (Placeholder) |

## Data Structure → 使用地点

| Data Structure | 变量名 | 所在 Controller | 存什么 |
|---------------|--------|----------------|--------|
| BST | `masterGuestRegistry` | App → 共享 | Guest (by confirmationNumber) |
| BST | `guestTree` | FrontDeskController | 同上的引用 |
| BST | `roomTree` | FrontDeskController | Room (by roomNumber) |
| Queue | `waitingQueue` | BookingController | 等待分配房间的 Guest |
| Stack | `taskLogStack` | HousekeepingController | 清洁状态变更日志 |
| ArrayList | `sharedRoomList` | App → 共享 | 所有 Room |
| ArrayList | `bookingList` | BookingController | 所有 Booking 记录 |
| ArrayList | `registeredGuests` | BookingController | 通过 Booking 登记的 Guest |
| ArrayList | `activeCheckedInConfirmations` | FrontDeskController | 已入住的确认号 |

## Algorithm → 使用地点

| Algorithm | 使用方法 | Complexity |
|-----------|---------|------------|
| BST Search | `searchGuestByConfirmationNumber()`, `searchRoomByNumber()` | O(log n) |
| BST Insert | `registerGuest()`, `registerWalkInGuest()` | O(log n) |
| BST Delete | `removeGuest()` | O(log n) |
| BST Range Search | `searchGuestsByConfirmationRange()` | O(log n + k) |
| BST Traversals | `searchGuestsByName()`, `getAllRooms()`, reports | O(n) |
| BST Rebalance | `rebalanceTrees()` | O(n) |
| Selection Sort | 所有 `sort()` 调用 (MyArrayList.sort) | O(n²) |
| Linear Search | `findRoomByNumber()`, `findGuestByIC()`, `searchGuestsByName()` | O(n) |
| Queue FIFO | `enqueue()`, `dequeue()` | O(1) |
| Stack LIFO | `push()`, `pop()` | O(1) |

---

## Program Execution Flow（完整执行流程）

```
 1. Program Start → JVM 调用 App.main()
 2. 创建共享数据 → BST<Guest> + MyArrayList<Room>
 3. seedMasterData() → 预填 7 间房 + 6 个客人
 4. 创建 4 个 UI → BookingUI, HousekeepingUI, FrontDeskUI, LoyaltyUI
 5. 显示主菜单 → do-while 循环

 ┌─ 用户选 1 → Booking 模块
 │   ├── 注册 Walk-In → enqueue 到 Queue
 │   ├── 查看队列 → toList() 快照
 │   ├── 处理下一位 → dequeue + 创建 Booking + Room="Reserved"
 │   ├── 取消预订 → Booking="Cancelled" + Room="Ready"
 │   └── 生成报告 → Filter + Selection Sort
 │
 ├─ 用户选 2 → Housekeeping 模块
 │   ├── 查看房间 → 列出所有 Room
 │   ├── 推进清洁 → statusIndex++ → push log
 │   ├── 手动设置 → 直接改 status → push log
 │   ├── 撤销 → pop log → 恢复 previousStatus
 │   └── 报告 → 统计/筛选/排序
 │
 ├─ 用户选 3 → Front Desk 模块
 │   ├── 搜索客人 → BST Search (O(log n)) / Name/IC Linear Search
 │   ├── 注册/删除 → BST add/remove
 │   ├── 入住 → 验证 + Room="Occupied" + Guest="CheckedIn"
 │   ├── 换房 → 旧房="Dirty" + 新房="Occupied"
 │   ├── 账单退房 → 计算折扣 + 积分 + Room="Dirty" + Guest="CheckedOut"
 │   ├── 报告 → Filter + Sort
 │   └── BST 诊断 → 可视化/遍历比较/重新平衡
 │
 ├─ 用户选 4 → Loyalty 模块 (Placeholder)
 │
 └─ 用户选 0 → 退出程序
```

---

## 🎯 Viva / Presentation 最需要理解的 10 个重点

### 1. 为什么用 BST 来存 Guest？
> 因为 BST 支持 O(log n) 的搜索效率。当客人数量很多时，BST 比 ArrayList 的 O(n) 线性搜索快得多。BST 还支持 Range Search 和有序遍历。

### 2. BST 的 Key 是什么？怎么比较？
> Key 是 `confirmationNumber`。通过 `Guest.compareTo()` 方法用字典序 (`compareToIgnoreCase`) 比较。所有 BST 的 add/search/remove 都依赖这个比较。

### 3. 为什么有些搜索是 O(log n)，有些是 O(n)？
> 按 **confirmationNumber** 搜索 = O(log n)，因为它是 BST 的 Key。按 **Name** 或 **IC** 搜索 = O(n)，因为它们不是 Key，必须遍历整棵树再逐一比较。

### 4. Queue 在哪里用？为什么用 Queue 而不是 Stack？
> Queue 用在 **Booking 等待队列**。因为客人需要 **先来先服务 (FIFO)**。如果用 Stack，最后来的人反而最先被服务，不公平。

### 5. Stack 在哪里用？为什么用 Stack 而不是 Queue？
> Stack 用在 **Housekeeping 的操作日志**。因为撤销时需要 **最后的操作最先被撤回 (LIFO)**。如果用 Queue，撤销的顺序就乱了。

### 6. BST 删除节点有哪三种情况？
> ① 叶子节点 → 直接删除  
> ② 只有一个子节点 → 用子节点替代  
> ③ 两个子节点 → 找右子树的最小值 (In-Order Successor) 替代，再递归删除 successor

### 7. Rebalance 是怎么做的？为什么需要？
> 当按顺序插入时 BST 会退化成链表（高度 = n），搜索变成 O(n)。Rebalance 把所有元素取出排序，然后从中间元素开始递归重建，让树高度变成 O(log n)。

### 8. 这个 Program 怎么实现多模块共享数据？
> `App.main()` 创建一棵 BST 和一个 ArrayList，然后把它们的 **引用 (reference)** 传给所有 Controller。所有模块操作的是同一个对象，所以一个模块改了数据，其他模块立刻可以看到。

### 9. MyArrayList 的 sort() 用的什么算法？
> **Selection Sort（选择排序）**。每轮从未排序部分找出最小元素，和当前位置交换。时间复杂度 O(n²)。

### 10. 系统有哪些 Business Rule（业务规则）？
> ① 客人必须按 FIFO 顺序处理  
> ② 只能入住 "Ready for Check-In" 的房间  
> ③ 同一客人不能同时入住两间房  
> ④ Platinum/Gold 会员可享受免费升级和折扣 (20%/10%/5%)  
> ⑤ 退房后房间自动变 "Dirty" 等待清洁  
> ⑥ 清洁流程必须按 Dirty → Cleaning → Inspected → Ready 顺序进行  
> ⑦ 清洁操作可以撤销（Stack rollback）
