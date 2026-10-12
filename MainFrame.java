import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

// ============================================================
// MainFrame.java
// Java Swing Frontend for Smart Public Transportation System
// Connects to the existing backend classes in Main.java
// ============================================================

public class MainFrame extends JFrame {

    // ---------------------------------------------------------
    // THEME COLORS
    // ---------------------------------------------------------
    private static final Color BG_DARK       = new Color(18, 18, 30);
    private static final Color BG_CARD       = new Color(28, 28, 48);
    private static final Color BG_SIDEBAR    = new Color(22, 22, 38);
    private static final Color BG_INPUT      = new Color(38, 38, 62);
    private static final Color TEXT_PRIMARY   = new Color(230, 230, 250);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 190);
    private static final Color ACCENT_BLUE   = new Color(80, 120, 255);
    private static final Color ACCENT_GREEN  = new Color(46, 204, 113);
    private static final Color ACCENT_ORANGE = new Color(255, 165, 50);
    private static final Color ACCENT_RED    = new Color(231, 76, 60);
    private static final Color ACCENT_PURPLE = new Color(155, 89, 230);
    private static final Color ACCENT_CYAN   = new Color(52, 199, 220);
    private static final Color TABLE_HEADER  = new Color(35, 35, 58);
    private static final Color TABLE_ROW_ALT = new Color(25, 25, 42);
    private static final Color BORDER_COLOR  = new Color(55, 55, 80);

    private static final Font FONT_TITLE     = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SUBTITLE  = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_BODY      = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_SMALL     = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_STAT      = new Font("Segoe UI", Font.BOLD, 36);
    private static final Font FONT_SIDEBAR   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_HEADING   = new Font("Segoe UI", Font.BOLD, 28);

    // ---------------------------------------------------------
    // BACKEND SERVICE (single shared instance)
    // ---------------------------------------------------------
    private TransportService service;
    private int demandArrivalCounter = 1;

    // ---------------------------------------------------------
    // UI COMPONENTS
    // ---------------------------------------------------------
    private CardLayout cardLayout;
    private JPanel contentPanel;

    // Dashboard stat labels
    private JLabel lblTotalStops;
    private JLabel lblTotalBuses;
    private JLabel lblAvailBuses;
    private JLabel lblWaitingDemands;

    // Tables
    private JTable stopTable;
    private DefaultTableModel stopTableModel;

    private JTable busTable;
    private DefaultTableModel busTableModel;

    private JTable demandTable;
    private DefaultTableModel demandTableModel;

    // Demand form combo boxes
    private JComboBox<String> cmbPickup;
    private JComboBox<String> cmbDestination;

    // Sidebar buttons for highlighting active screen
    private JButton[] sidebarButtons;
    private int activeButtonIndex = 0;

    // ---------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------
    public MainFrame() {

        // Initialize backend service
        service = new TransportService();
        service.loadFromDatabase();
        if (service.getStops().isEmpty() && service.getBuses().isEmpty()) {
            loadSampleData();
        }

        // Frame setup
        setTitle("Smart Public Transportation Route Optimization");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        // Main layout: Sidebar + Content
        setLayout(new BorderLayout());
        add(createSidebar(), BorderLayout.WEST);
        add(createContentPanel(), BorderLayout.CENTER);

        // Refresh dashboard on startup
        refreshDashboard();
    }

    // ---------------------------------------------------------
    // LOAD SAMPLE DATA (same as Main.java)
    // ---------------------------------------------------------
    private void loadSampleData() {

        service.addStop(new Stop(1, "Central Bus Stand"));
        service.addStop(new Stop(2, "Market"));
        service.addStop(new Stop(3, "College"));
        service.addStop(new Stop(4, "Hospital"));
        service.addStop(new Stop(5, "Railway Station"));

        service.addBus(new MiniBus(101, 15));
        service.addBus(new StandardBus(102, 30));
        service.addBus(new StandardBus(103, 40));

        service.addDemand(new DemandRequest(
            1, service.getStop(1), service.getStop(3), 20, demandArrivalCounter++
        ));
        service.addDemand(new DemandRequest(
            2, service.getStop(2), service.getStop(5), 10, demandArrivalCounter++
        ));
    }

    // =========================================================
    // SIDEBAR
    // =========================================================
    private JPanel createSidebar() {

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(
            0, 0, 0, 1, BORDER_COLOR
        ));

        // Logo area
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(BG_SIDEBAR);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(24, 18, 18, 18));

        JLabel logoIcon = new JLabel("\u2584 \u2588 \u2584");
        logoIcon.setFont(new Font("Consolas", Font.BOLD, 18));
        logoIcon.setForeground(ACCENT_BLUE);

        JLabel logoText = new JLabel("  TransitPro");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logoText.setForeground(TEXT_PRIMARY);

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoRow.setBackground(BG_SIDEBAR);
        logoRow.add(logoIcon);
        logoRow.add(logoText);
        logoPanel.add(logoRow, BorderLayout.NORTH);

        JLabel tagline = new JLabel("Route Optimization System");
        tagline.setFont(FONT_SMALL);
        tagline.setForeground(TEXT_SECONDARY);
        tagline.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 0));
        logoPanel.add(tagline, BorderLayout.SOUTH);

        sidebar.add(logoPanel);

        // Separator
        sidebar.add(createSeparator());
        sidebar.add(Box.createVerticalStrut(8));

        // Menu label
        JLabel menuLabel = new JLabel("   NAVIGATION");
        menuLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        menuLabel.setForeground(TEXT_SECONDARY);
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(menuLabel);
        sidebar.add(Box.createVerticalStrut(8));

        // Navigation buttons
        String[] labels = {
            "\u25C9  Dashboard",
            "\u25A3  Bus Stops",
            "\u25B6  Bus Management",
            "\u25CB  Passenger Demands",
            "\u2713  Bus Allocation",
            "\u21BA  Complete Trip"
        };

        String[] cardNames = {
            "dashboard", "stops", "buses", "demands", "allocate", "complete"
        };

        sidebarButtons = new JButton[labels.length];

        for (int i = 0; i < labels.length; i++) {

            final int index = i;
            final String card = cardNames[i];

            JButton btn = createSidebarButton(labels[i]);
            sidebarButtons[i] = btn;

            btn.addActionListener(e -> {
                switchToCard(card, index);
            });

            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(2));
        }

        // Set first button active
        highlightSidebarButton(0);

        sidebar.add(Box.createVerticalGlue());

        // Version label at bottom
        JLabel version = new JLabel("  v1.0  |  Java Swing");
        version.setFont(FONT_SMALL);
        version.setForeground(new Color(80, 80, 110));
        version.setBorder(BorderFactory.createEmptyBorder(0, 12, 18, 0));
        sidebar.add(version);

        return sidebar;
    }

    private JButton createSidebarButton(String text) {

        JButton btn = new JButton(text);
        btn.setFont(FONT_SIDEBAR);
        btn.setForeground(TEXT_SECONDARY);
        btn.setBackground(BG_SIDEBAR);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(230, 44));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.getBackground() != ACCENT_BLUE) {
                    btn.setBackground(new Color(35, 35, 58));
                    btn.setForeground(TEXT_PRIMARY);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.getBackground() != ACCENT_BLUE) {
                    btn.setBackground(BG_SIDEBAR);
                    btn.setForeground(TEXT_SECONDARY);
                }
            }
        });

        return btn;
    }

    private void highlightSidebarButton(int index) {

        for (int i = 0; i < sidebarButtons.length; i++) {
            if (i == index) {
                sidebarButtons[i].setBackground(ACCENT_BLUE);
                sidebarButtons[i].setForeground(Color.WHITE);
            } else {
                sidebarButtons[i].setBackground(BG_SIDEBAR);
                sidebarButtons[i].setForeground(TEXT_SECONDARY);
            }
        }
        activeButtonIndex = index;
    }

    private void switchToCard(String name, int index) {
        cardLayout.show(contentPanel, name);
        highlightSidebarButton(index);

        // Refresh data when navigating
        if (name.equals("dashboard"))  refreshDashboard();
        if (name.equals("stops"))      refreshStopTable();
        if (name.equals("buses"))      refreshBusTable();
        if (name.equals("demands"))    { refreshDemandTable(); refreshStopCombos(); }
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        sep.setForeground(BORDER_COLOR);
        sep.setBackground(BG_SIDEBAR);
        sep.setMaximumSize(new Dimension(230, 1));
        return sep;
    }

    // =========================================================
    // CONTENT PANEL (CardLayout)
    // =========================================================
    private JPanel createContentPanel() {

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_DARK);

        contentPanel.add(createDashboardPanel(),  "dashboard");
        contentPanel.add(createStopsPanel(),      "stops");
        contentPanel.add(createBusPanel(),        "buses");
        contentPanel.add(createDemandsPanel(),    "demands");
        contentPanel.add(createAllocatePanel(),   "allocate");
        contentPanel.add(createCompletePanel(),   "complete");

        return contentPanel;
    }

    // =========================================================
    // 1. DASHBOARD PANEL
    // =========================================================
    private JPanel createDashboardPanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Header
        JPanel header = createScreenHeader(
            "Dashboard",
            "Real-time overview of your transport system"
        );
        panel.add(header, BorderLayout.NORTH);

        // Stats grid
        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        statsGrid.setBackground(BG_DARK);

        lblTotalStops    = new JLabel("0", SwingConstants.CENTER);
        lblTotalBuses    = new JLabel("0", SwingConstants.CENTER);
        lblAvailBuses    = new JLabel("0", SwingConstants.CENTER);
        lblWaitingDemands = new JLabel("0", SwingConstants.CENTER);

        statsGrid.add(createStatCard("Total Stops",      lblTotalStops,    ACCENT_BLUE,   "\u2302"));
        statsGrid.add(createStatCard("Total Buses",       lblTotalBuses,    ACCENT_PURPLE, "\u25CF"));
        statsGrid.add(createStatCard("Available Buses",   lblAvailBuses,    ACCENT_GREEN,  "\u2713"));
        statsGrid.add(createStatCard("Waiting Demands",   lblWaitingDemands, ACCENT_ORANGE, "\u25B2"));

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setBackground(BG_DARK);
        centerWrap.add(statsGrid, BorderLayout.NORTH);

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(BG_CARD);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        JLabel infoTitle = new JLabel("\u25C9  Quick Guide");
        infoTitle.setFont(FONT_SUBTITLE);
        infoTitle.setForeground(TEXT_PRIMARY);

        JTextArea infoText = new JTextArea();
        infoText.setFont(FONT_BODY);
        infoText.setForeground(TEXT_SECONDARY);
        infoText.setBackground(BG_CARD);
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setText(
            "\u2022  Use the sidebar to navigate between screens.\n" +
            "\u2022  Add bus stops, buses, and passenger demands using the forms.\n" +
            "\u2022  Allocate buses to waiting demands based on priority.\n" +
            "\u2022  Complete trips to make buses available again.\n" +
            "\u2022  Dashboard statistics update automatically."
        );
        infoText.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        infoPanel.add(infoTitle, BorderLayout.NORTH);
        infoPanel.add(infoText, BorderLayout.CENTER);

        centerWrap.add(Box.createVerticalStrut(24), BorderLayout.CENTER);

        JPanel bottomWrap = new JPanel(new BorderLayout());
        bottomWrap.setBackground(BG_DARK);
        bottomWrap.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        bottomWrap.add(infoPanel, BorderLayout.CENTER);

        centerWrap.add(bottomWrap, BorderLayout.SOUTH);
        panel.add(centerWrap, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel,
                                   Color accentColor, String icon) {

        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Top row: icon + title
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topRow.setBackground(BG_CARD);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        iconLabel.setForeground(accentColor);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_SMALL);
        titleLabel.setForeground(TEXT_SECONDARY);

        topRow.add(iconLabel);
        topRow.add(titleLabel);

        // Value
        valueLabel.setFont(FONT_STAT);
        valueLabel.setForeground(accentColor);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 0));

        card.add(topRow, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // =========================================================
    // 2. BUS STOPS PANEL
    // =========================================================
    private JPanel createStopsPanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        panel.add(createScreenHeader(
            "Bus Stops",
            "View and manage all bus stops in the network"
        ), BorderLayout.NORTH);

        // Table
        stopTableModel = new DefaultTableModel(
            new String[]{"Stop ID", "Stop Name"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        stopTable = createStyledTable(stopTableModel);
        JScrollPane scrollPane = createStyledScrollPane(stopTable);

        // Form
        JPanel form = createFormPanel("Add New Stop");

        JTextField tfStopId   = createStyledTextField("Stop ID");
        JTextField tfStopName = createStyledTextField("Stop Name");

        JButton btnAdd = createAccentButton("Add Stop", ACCENT_GREEN);

        btnAdd.addActionListener(e -> {
            try {
                String idText = tfStopId.getText().trim();
                String name   = tfStopName.getText().trim();

                if (idText.isEmpty() || name.isEmpty()) {
                    showError("Please fill in all fields.");
                    return;
                }

                int stopId = Integer.parseInt(idText);

                if (service.getStop(stopId) != null) {
                    showError("Stop ID " + stopId + " already exists.");
                    return;
                }

                service.addStop(new Stop(stopId, name));
                showSuccess("Stop \"" + name + "\" added successfully!");

                tfStopId.setText("");
                tfStopName.setText("");

                refreshStopTable();
                refreshDashboard();

            } catch (NumberFormatException ex) {
                showError("Stop ID must be a number.");
            }
        });

        form.add(createFormRow("Stop ID:", tfStopId));
        form.add(Box.createVerticalStrut(10));
        form.add(createFormRow("Stop Name:", tfStopName));
        form.add(Box.createVerticalStrut(16));
        form.add(btnAdd);

        // Split layout
        JPanel splitPanel = new JPanel(new BorderLayout(16, 0));
        splitPanel.setBackground(BG_DARK);
        splitPanel.add(scrollPane, BorderLayout.CENTER);
        splitPanel.add(form, BorderLayout.EAST);

        panel.add(splitPanel, BorderLayout.CENTER);

        refreshStopTable();
        return panel;
    }

    // =========================================================
    // 3. BUS MANAGEMENT PANEL
    // =========================================================
    private JPanel createBusPanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        panel.add(createScreenHeader(
            "Bus Management",
            "View fleet status and add new buses"
        ), BorderLayout.NORTH);

        // Table
        busTableModel = new DefaultTableModel(
            new String[]{"Bus ID", "Type", "Capacity", "Available"}, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        busTable = createStyledTable(busTableModel);
        JScrollPane scrollPane = createStyledScrollPane(busTable);

        // Form
        JPanel form = createFormPanel("Add New Bus");

        JTextField tfBusId    = createStyledTextField("Bus ID");
        JTextField tfCapacity = createStyledTextField("Capacity");

        JComboBox<String> cmbType = new JComboBox<>(
            new String[]{"MiniBus", "StandardBus"}
        );
        styleComboBox(cmbType);

        JButton btnAdd = createAccentButton("Add Bus", ACCENT_BLUE);

        btnAdd.addActionListener(e -> {
            try {
                String idText  = tfBusId.getText().trim();
                String capText = tfCapacity.getText().trim();

                if (idText.isEmpty() || capText.isEmpty()) {
                    showError("Please fill in all fields.");
                    return;
                }

                int busId    = Integer.parseInt(idText);
                int capacity = Integer.parseInt(capText);

                if (capacity <= 0) {
                    showError("Capacity must be a positive number.");
                    return;
                }

                if (service.getBuses().containsKey(busId)) {
                    showError("Bus ID " + busId + " already exists.");
                    return;
                }

                String type = (String) cmbType.getSelectedItem();
                Bus newBus;

                if ("MiniBus".equals(type)) {
                    newBus = new MiniBus(busId, capacity);
                } else {
                    newBus = new StandardBus(busId, capacity);
                }

                service.addBus(newBus);
                showSuccess(type + " (ID: " + busId + ") added successfully!");

                tfBusId.setText("");
                tfCapacity.setText("");

                refreshBusTable();
                refreshDashboard();

            } catch (NumberFormatException ex) {
                showError("Bus ID and Capacity must be numbers.");
            }
        });

        form.add(createFormRow("Bus ID:", tfBusId));
        form.add(Box.createVerticalStrut(10));
        form.add(createFormRow("Type:", cmbType));
        form.add(Box.createVerticalStrut(10));
        form.add(createFormRow("Capacity:", tfCapacity));
        form.add(Box.createVerticalStrut(16));
        form.add(btnAdd);

        JPanel splitPanel = new JPanel(new BorderLayout(16, 0));
        splitPanel.setBackground(BG_DARK);
        splitPanel.add(scrollPane, BorderLayout.CENTER);
        splitPanel.add(form, BorderLayout.EAST);

        panel.add(splitPanel, BorderLayout.CENTER);

        refreshBusTable();
        return panel;
    }

    // =========================================================
    // 4. PASSENGER DEMANDS PANEL
    // =========================================================
    private JPanel createDemandsPanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        panel.add(createScreenHeader(
            "Passenger Demands",
            "Add demand requests and view waiting queue"
        ), BorderLayout.NORTH);

        // Table
        demandTableModel = new DefaultTableModel(
            new String[]{
                "Request ID", "Pickup Stop", "Destination",
                "Passengers", "Priority"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        demandTable = createStyledTable(demandTableModel);
        JScrollPane scrollPane = createStyledScrollPane(demandTable);

        // Form
        JPanel form = createFormPanel("Add New Demand");

        JTextField tfReqId      = createStyledTextField("Request ID");
        JTextField tfPassengers = createStyledTextField("Passenger Count");

        cmbPickup      = new JComboBox<>();
        cmbDestination = new JComboBox<>();
        styleComboBox(cmbPickup);
        styleComboBox(cmbDestination);

        JButton btnAdd = createAccentButton("Add Demand", ACCENT_ORANGE);

        btnAdd.addActionListener(e -> {
            try {
                String reqText  = tfReqId.getText().trim();
                String passText = tfPassengers.getText().trim();

                if (reqText.isEmpty() || passText.isEmpty()) {
                    showError("Please fill in all fields.");
                    return;
                }

                if (cmbPickup.getItemCount() == 0) {
                    showError("No stops available. Add stops first.");
                    return;
                }

                int requestId  = Integer.parseInt(reqText);
                int passengers = Integer.parseInt(passText);

                if (passengers <= 0) {
                    showError("Passenger count must be positive.");
                    return;
                }

                if (service.getDemands().containsKey(requestId)) {
                    showError("Request ID " + requestId + " already exists.");
                    return;
                }

                // Parse stop ID from combo box selection "ID - Name"
                String pickupSel = (String) cmbPickup.getSelectedItem();
                String destSel   = (String) cmbDestination.getSelectedItem();

                if (pickupSel == null || destSel == null) {
                    showError("Please select pickup and destination stops.");
                    return;
                }

                int pickupId = Integer.parseInt(
                    pickupSel.split(" - ")[0].trim()
                );
                int destId   = Integer.parseInt(
                    destSel.split(" - ")[0].trim()
                );

                if (pickupId == destId) {
                    showError("Pickup and destination cannot be the same.");
                    return;
                }

                Stop pickup = service.getStop(pickupId);
                Stop dest   = service.getStop(destId);

                if (pickup == null || dest == null) {
                    showError("Invalid stop selection.");
                    return;
                }

                DemandRequest demand = new DemandRequest(
                    requestId, pickup, dest, passengers,
                    demandArrivalCounter++
                );

                service.addDemand(demand);
                showSuccess("Demand request #" + requestId + " added!");

                tfReqId.setText("");
                tfPassengers.setText("");

                refreshDemandTable();
                refreshDashboard();

            } catch (NumberFormatException ex) {
                showError("Request ID and Passengers must be numbers.");
            }
        });

        form.add(createFormRow("Request ID:", tfReqId));
        form.add(Box.createVerticalStrut(10));
        form.add(createFormRow("Pickup Stop:", cmbPickup));
        form.add(Box.createVerticalStrut(10));
        form.add(createFormRow("Destination:", cmbDestination));
        form.add(Box.createVerticalStrut(10));
        form.add(createFormRow("Passengers:", tfPassengers));
        form.add(Box.createVerticalStrut(16));
        form.add(btnAdd);

        JPanel splitPanel = new JPanel(new BorderLayout(16, 0));
        splitPanel.setBackground(BG_DARK);
        splitPanel.add(scrollPane, BorderLayout.CENTER);
        splitPanel.add(form, BorderLayout.EAST);

        panel.add(splitPanel, BorderLayout.CENTER);

        refreshDemandTable();
        refreshStopCombos();
        return panel;
    }

    // =========================================================
    // 5. BUS ALLOCATION PANEL
    // =========================================================
    private JPanel createAllocatePanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        panel.add(createScreenHeader(
            "Bus Allocation",
            "Allocate available buses to the highest-priority demand"
        ), BorderLayout.NORTH);

        // Center card
        JPanel centerCard = new JPanel();
        centerCard.setLayout(new BoxLayout(centerCard, BoxLayout.Y_AXIS));
        centerCard.setBackground(BG_CARD);
        centerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));

        JLabel allocIcon = new JLabel("\u2713");
        allocIcon.setFont(new Font("Segoe UI", Font.BOLD, 60));
        allocIcon.setForeground(ACCENT_GREEN);
        allocIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel allocTitle = new JLabel("Allocate a Bus");
        allocTitle.setFont(FONT_TITLE);
        allocTitle.setForeground(TEXT_PRIMARY);
        allocTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel allocDesc = new JLabel(
            "The bus with available capacity will be assigned to the highest-priority demand."
        );
        allocDesc.setFont(FONT_BODY);
        allocDesc.setForeground(TEXT_SECONDARY);
        allocDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Result area
        JTextArea resultArea = new JTextArea(6, 40);
        resultArea.setFont(FONT_BODY);
        resultArea.setForeground(TEXT_PRIMARY);
        resultArea.setBackground(BG_INPUT);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        resultScroll.setMaximumSize(new Dimension(600, 180));
        resultScroll.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnAllocate = createAccentButton("  Allocate Bus  ", ACCENT_GREEN);
        btnAllocate.setFont(FONT_SUBTITLE);
        btnAllocate.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnAllocate.addActionListener(e -> {

            // Capture console output from allocateBus()
            String output = captureConsoleOutput(() -> {
                service.allocateBus();
            });

            resultArea.setText(output.isEmpty() ? "No output." : output);

            refreshBusTable();
            refreshDemandTable();
            refreshDashboard();
        });

        centerCard.add(allocIcon);
        centerCard.add(Box.createVerticalStrut(12));
        centerCard.add(allocTitle);
        centerCard.add(Box.createVerticalStrut(8));
        centerCard.add(allocDesc);
        centerCard.add(Box.createVerticalStrut(24));
        centerCard.add(btnAllocate);
        centerCard.add(Box.createVerticalStrut(20));
        centerCard.add(resultScroll);

        // Wrap center card
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.add(centerCard);

        panel.add(wrapper, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================
    // 6. COMPLETE BUS TRIP PANEL
    // =========================================================
    private JPanel createCompletePanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        panel.add(createScreenHeader(
            "Complete Bus Trip",
            "Mark a bus trip as complete to make it available again"
        ), BorderLayout.NORTH);

        // Center card
        JPanel centerCard = new JPanel();
        centerCard.setLayout(new BoxLayout(centerCard, BoxLayout.Y_AXIS));
        centerCard.setBackground(BG_CARD);
        centerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));

        JLabel tripIcon = new JLabel("\u21BA");
        tripIcon.setFont(new Font("Segoe UI", Font.BOLD, 60));
        tripIcon.setForeground(ACCENT_CYAN);
        tripIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tripTitle = new JLabel("Complete a Trip");
        tripTitle.setFont(FONT_TITLE);
        tripTitle.setForeground(TEXT_PRIMARY);
        tripTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tripDesc = new JLabel(
            "Enter the Bus ID below to mark its current trip as completed."
        );
        tripDesc.setFont(FONT_BODY);
        tripDesc.setForeground(TEXT_SECONDARY);
        tripDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField tfBusId = createStyledTextField("Bus ID");
        tfBusId.setMaximumSize(new Dimension(300, 38));
        tfBusId.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Result area
        JTextArea resultArea = new JTextArea(4, 40);
        resultArea.setFont(FONT_BODY);
        resultArea.setForeground(TEXT_PRIMARY);
        resultArea.setBackground(BG_INPUT);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        resultScroll.setMaximumSize(new Dimension(600, 140));
        resultScroll.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnComplete = createAccentButton(
            "  Complete Trip  ", ACCENT_CYAN
        );
        btnComplete.setFont(FONT_SUBTITLE);
        btnComplete.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnComplete.addActionListener(e -> {

            String idText = tfBusId.getText().trim();

            if (idText.isEmpty()) {
                showError("Please enter a Bus ID.");
                return;
            }

            try {
                int busId = Integer.parseInt(idText);

                String output = captureConsoleOutput(() -> {
                    service.completeBusTrip(busId);
                });

                resultArea.setText(
                    output.isEmpty() ? "No output." : output
                );

                tfBusId.setText("");
                refreshBusTable();
                refreshDashboard();

            } catch (NumberFormatException ex) {
                showError("Bus ID must be a number.");
            }
        });

        centerCard.add(tripIcon);
        centerCard.add(Box.createVerticalStrut(12));
        centerCard.add(tripTitle);
        centerCard.add(Box.createVerticalStrut(8));
        centerCard.add(tripDesc);
        centerCard.add(Box.createVerticalStrut(24));
        centerCard.add(tfBusId);
        centerCard.add(Box.createVerticalStrut(14));
        centerCard.add(btnComplete);
        centerCard.add(Box.createVerticalStrut(20));
        centerCard.add(resultScroll);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.add(centerCard);

        panel.add(wrapper, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================
    // REFRESH METHODS
    // =========================================================

    private void refreshDashboard() {

        int totalStops = service.getStops().size();
        int totalBuses = service.getBuses().size();

        int availBuses = 0;
        for (Bus bus : service.getBuses().values()) {
            if (bus.isAvailable()) availBuses++;
        }

        int waitingDemands = service.getDemandQueue().size();

        lblTotalStops.setText(String.valueOf(totalStops));
        lblTotalBuses.setText(String.valueOf(totalBuses));
        lblAvailBuses.setText(String.valueOf(availBuses));
        lblWaitingDemands.setText(String.valueOf(waitingDemands));
    }

    private void refreshStopTable() {

        stopTableModel.setRowCount(0);

        // Sort stops by ID for consistent display
        java.util.List<Stop> sorted = new ArrayList<>(
            service.getStops().values()
        );
        sorted.sort(Comparator.comparingInt(Stop::getStopId));

        for (Stop stop : sorted) {
            stopTableModel.addRow(new Object[]{
                stop.getStopId(),
                stop.getStopName()
            });
        }
    }

    private void refreshBusTable() {

        busTableModel.setRowCount(0);

        java.util.List<Bus> sorted = new ArrayList<>(
            service.getBuses().values()
        );
        sorted.sort(Comparator.comparingInt(Bus::getBusId));

        for (Bus bus : sorted) {
            busTableModel.addRow(new Object[]{
                bus.getBusId(),
                bus.getBusType(),
                bus.getCapacity(),
                bus.isAvailable() ? "Yes" : "No"
            });
        }
    }

    private void refreshDemandTable() {

        demandTableModel.setRowCount(0);

        // Use a copy of the PriorityQueue for ordered display
        PriorityQueue<DemandRequest> copy = new PriorityQueue<>(
            service.getDemandQueue()
        );

        int priority = 1;
        while (!copy.isEmpty()) {
            DemandRequest d = copy.poll();
            demandTableModel.addRow(new Object[]{
                d.getRequestId(),
                d.getPickupStop().getStopName(),
                d.getDestination().getStopName(),
                d.getPassengers(),
                "#" + priority++
            });
        }
    }

    private void refreshStopCombos() {

        if (cmbPickup == null || cmbDestination == null) return;

        cmbPickup.removeAllItems();
        cmbDestination.removeAllItems();

        java.util.List<Stop> sorted = new ArrayList<>(
            service.getStops().values()
        );
        sorted.sort(Comparator.comparingInt(Stop::getStopId));

        for (Stop stop : sorted) {
            String item = stop.getStopId() + " - " + stop.getStopName();
            cmbPickup.addItem(item);
            cmbDestination.addItem(item);
        }
    }

    // =========================================================
    // UI HELPER METHODS
    // =========================================================

    private JPanel createScreenHeader(String title, String subtitle) {

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_HEADING);
        lblTitle.setForeground(TEXT_PRIMARY);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(FONT_BODY);
        lblSub.setForeground(TEXT_SECONDARY);
        lblSub.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);

        return header;
    }

    private JPanel createFormPanel(String title) {

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setPreferredSize(new Dimension(280, 0));
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_SUBTITLE);
        lblTitle.setForeground(TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblTitle);
        form.add(Box.createVerticalStrut(16));

        return form;
    }

    private JPanel createFormRow(String label, JComponent field) {

        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setBackground(BG_CARD);
        row.setMaximumSize(new Dimension(260, 60));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_SECONDARY);

        row.add(lbl, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);

        return row;
    }

    private JTextField createStyledTextField(String placeholder) {

        JTextField tf = new JTextField();
        tf.setFont(FONT_BODY);
        tf.setForeground(TEXT_PRIMARY);
        tf.setBackground(BG_INPUT);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setPreferredSize(new Dimension(0, 36));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        return tf;
    }

    private void styleComboBox(JComboBox<String> cmb) {

        cmb.setFont(FONT_BODY);
        cmb.setForeground(TEXT_PRIMARY);
        cmb.setBackground(BG_INPUT);
        cmb.setPreferredSize(new Dimension(0, 36));
        cmb.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        cmb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
                );
                setBackground(isSelected ? ACCENT_BLUE : BG_INPUT);
                setForeground(TEXT_PRIMARY);
                setFont(FONT_BODY);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
    }

    private JButton createAccentButton(String text, Color color) {

        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(260, 42));

        Color hoverColor = color.brighter();
        Color pressColor = color.darker();

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                btn.setBackground(pressColor);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                btn.setBackground(hoverColor);
            }
        });

        return btn;
    }

    private JTable createStyledTable(DefaultTableModel model) {

        JTable table = new JTable(model);
        table.setFont(FONT_BODY);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_CARD);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(36);
        table.setSelectionBackground(ACCENT_BLUE);
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BODY_BOLD);
        header.setForeground(TEXT_PRIMARY);
        header.setBackground(TABLE_HEADER);
        header.setBorder(BorderFactory.createMatteBorder(
            0, 0, 2, 0, ACCENT_BLUE
        ));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 40));

        // Alternate row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                JTable tbl, Object value, boolean isSelected,
                boolean hasFocus, int row, int col
            ) {
                super.getTableCellRendererComponent(
                    tbl, value, isSelected, hasFocus, row, col
                );
                if (!isSelected) {
                    setBackground(
                        row % 2 == 0 ? BG_CARD : TABLE_ROW_ALT
                    );
                    setForeground(TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        return table;
    }

    private JScrollPane createStyledScrollPane(JTable table) {

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scroll.getViewport().setBackground(BG_CARD);
        return scroll;
    }

    // =========================================================
    // UTILITY METHODS
    // =========================================================

    /**
     * Captures System.out output from backend methods that
     * print to console (e.g., allocateBus, completeBusTrip).
     */
    private String captureConsoleOutput(Runnable action) {

        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(baos);

        System.setOut(captureStream);
        try {
            action.run();
        } finally {
            System.setOut(originalOut);
        }

        return baos.toString().trim();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this, message,
            "Input Error", JOptionPane.ERROR_MESSAGE
        );
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(
            this, message,
            "Success", JOptionPane.INFORMATION_MESSAGE
        );
    }

    // =========================================================
    // MAIN METHOD - APPLICATION ENTRY POINT
    // =========================================================
    public static void main(String[] args) {

        // Set system look and feel for better native integration
        try {
            UIManager.setLookAndFeel(
                UIManager.getCrossPlatformLookAndFeelClassName()
            );
        } catch (Exception ignored) {}

        // Apply global dark theme to JOptionPane and other dialogs
        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("Panel.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Button.background", ACCENT_BLUE);
        UIManager.put("Button.foreground", Color.WHITE);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
