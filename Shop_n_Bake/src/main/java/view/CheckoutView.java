package view;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import model.Cake;
import model.User;
import model.OrderItem;
import util.Database;
import util.UIManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.util.stream.IntStream;
import java.util.Arrays;
import javax.swing.ButtonModel;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;

public class CheckoutView {
    private JFrame frame;
    private final User user;
    private final Map<Cake, Integer> cart;
    private final BigDecimal total;
    private final List<OrderItem> items;
    
    // Initialize fields before using them
    private JTextField streetField = new JTextField(20);
    private JTextField cityField = new JTextField(20);
    private JTextField zipField = new JTextField(10);
    private ButtonGroup deliveryGroup = new ButtonGroup();
    private ButtonGroup paymentGroup = new ButtonGroup();
    private JComboBox<String> timeSlotCombo;
    private JPanel addressPanel;
    private JComboBox<LocalDate> dayCombo;
    private JPanel paypalPanel;
    private JPanel creditCardPanel;
    private JPanel invoicePanel;
    private JTextField creditCardNumber;
    private JTextField cvcField;
    private JComboBox<String> expiryMonth;
    private JComboBox<String> expiryYear;
    private JCheckBox alternateEmailCheck;
    private JTextField alternateEmailField;

    public CheckoutView(User user, Map<Cake, Integer> cart, String address) {
        this.user = user;
        this.cart = cart;
        this.total = calculateTotal(cart);
        this.items = createOrderItems(cart);
        timeSlotCombo = new JComboBox<>(generateTimeSlots());
        initialize();
    }

    private void initialize() {
        frame = new JFrame("Checkout");
        frame.setLayout(new BorderLayout(10, 10));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        mainPanel.add(createOrderSummaryPanel());
        mainPanel.add(createDeliveryPanel());
        mainPanel.add(createPaymentPanel());
        
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(createButtonPanel(), BorderLayout.SOUTH);
        frame.setSize(500, 700);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backButton = UIManager.createStyledButton("Back");
        backButton.addActionListener(e -> {
            frame.dispose();
            new CustomerDashboard(user).display();
        });
        
        JButton confirmButton = UIManager.createStyledButton("Confirm Order");
        confirmButton.addActionListener(e -> {
            if (validateInputs()) {
                placeOrder();
            }
        });
        
        buttonPanel.add(backButton);
        buttonPanel.add(confirmButton);
        return buttonPanel;
    }

    private JPanel createOrderSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Order Summary"));

        String[] columns = {"Item", "Quantity", "Price", "Total"};
        Object[][] data = new Object[cart.size()][4];
        int i = 0;
        for (Map.Entry<Cake, Integer> entry : cart.entrySet()) {
            Cake cake = entry.getKey();
            Integer quantity = entry.getValue();
            BigDecimal itemTotal = cake.getPrice().multiply(BigDecimal.valueOf(quantity));
            
            data[i] = new Object[]{
                cake.getName(),
                quantity,
                String.format("€%.2f", cake.getPrice()),
                String.format("€%.2f", itemTotal)
            };
            i++;
        }

        JTable table = new JTable(data, columns);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        JLabel totalLabel = new JLabel(String.format("Total: €%.2f", total));
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.add(totalLabel);
        panel.add(totalPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDeliveryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Delivery Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Centered delivery type selection
        JPanel deliveryTypePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        deliveryGroup = new ButtonGroup();
        JRadioButton pickupButton = new JRadioButton("Pickup");
        JRadioButton deliveryButton = new JRadioButton("Delivery", true);
        
        pickupButton.addActionListener(e -> updateDeliveryOptions(true));
        deliveryButton.addActionListener(e -> updateDeliveryOptions(false));
        
        deliveryGroup.add(pickupButton);
        deliveryGroup.add(deliveryButton);
        deliveryTypePanel.add(pickupButton);
        deliveryTypePanel.add(deliveryButton);

        // Add components with gbc
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(deliveryTypePanel, gbc);

        // Date/time panel
        JPanel dateTimePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        dateTimePanel.setName("dateTimePanel");
        dayCombo = new JComboBox<>(generateAvailableDays());
        timeSlotCombo = new JComboBox<>(generateTimeSlots());
        
        dateTimePanel.add(new JLabel("Day:"));
        dateTimePanel.add(dayCombo);
        dateTimePanel.add(new JLabel("Time:"));
        dateTimePanel.add(timeSlotCombo);
        dateTimePanel.setVisible(false);

        gbc.gridy = 1;
        panel.add(dateTimePanel, gbc);

        // Address fields
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(new JLabel("Street:"), gbc);
        gbc.gridx = 1;
        panel.add(streetField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(new JLabel("City:"), gbc);
        gbc.gridx = 1;
        panel.add(cityField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        panel.add(new JLabel("ZIP:"), gbc);
        gbc.gridx = 1;
        panel.add(zipField, gbc);

        return panel;
    }

    private void updateDeliveryOptions(boolean isPickup) {
        JPanel mainPanel = (JPanel) frame.getContentPane().getComponent(0);
        JPanel deliveryPanel = (JPanel) mainPanel.getComponent(1);
        Component dateTimePanel = deliveryPanel.getComponent(1);
        dateTimePanel.setVisible(isPickup);
        addressPanel.setVisible(!isPickup);
        frame.revalidate();
        frame.repaint();
    }

    private JPanel createPaymentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Payment Method"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Payment method radio buttons
        paymentGroup = new ButtonGroup();
        String[] methods = {"PayPal", "Credit Card", "Invoice", "Cash"};
        
        for (int i = 0; i < methods.length; i++) {
            JRadioButton button = new JRadioButton(methods[i]);
            button.addActionListener(e -> updatePaymentFields(button.getText()));
            paymentGroup.add(button);
            gbc.gridx = 0;
            gbc.gridy = i;
            panel.add(button, gbc);
        }

        // PayPal panel
        paypalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton connectPayPalButton = UIManager.createStyledButton("Connect PayPal");
        paypalPanel.add(connectPayPalButton);
        paypalPanel.setVisible(false);

        // Credit Card panel with formatted input
        creditCardPanel = new JPanel(new GridBagLayout());
        creditCardNumber = new JFormattedTextField(createCardNumberFormatter());
        creditCardNumber.setColumns(19); // Space for 16 digits + 3 spaces
        cvcField = new JTextField(3);
        
        // Center-aligned expiry date
        JPanel expiryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        expiryMonth = new JComboBox<>(new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"});
        expiryYear = new JComboBox<>(generateYearRange());
        
        // Invoice panel with toggle
        invoicePanel = new JPanel(new GridBagLayout());
        alternateEmailCheck = new JCheckBox("Send invoice to different email");
        alternateEmailField = new JTextField(20);
        alternateEmailField.setVisible(false);
        
        alternateEmailCheck.addActionListener(e -> {
            alternateEmailField.setVisible(alternateEmailCheck.isSelected());
            invoicePanel.revalidate();
            invoicePanel.repaint();
        });

        // Add all payment panels
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 4;
        panel.add(paypalPanel, gbc);
        panel.add(creditCardPanel, gbc);
        panel.add(invoicePanel, gbc);

        return panel;
    }

    private MaskFormatter createCardNumberFormatter() {
        MaskFormatter formatter = null;
        try {
            formatter = new MaskFormatter("#### #### #### ####");
            formatter.setPlaceholderCharacter('_');
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return formatter;
    }

    private void setupCreditCardPanel(JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Card number
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Card Number:"), gbc);
        gbc.gridx = 1;
        panel.add(creditCardNumber, gbc);

        // CVC
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("CVC:"), gbc);
        gbc.gridx = 1;
        panel.add(cvcField, gbc);

        // Centered expiry date
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JPanel expiryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        expiryPanel.add(new JLabel("Expiry:"));
        expiryPanel.add(expiryMonth);
        expiryPanel.add(new JLabel("/"));
        expiryPanel.add(expiryYear);
        panel.add(expiryPanel, gbc);
    }

    private String[] generateTimeSlots() {
        ArrayList<String> slots = new ArrayList<>();
        // Morning slots: 8:00 - 12:00
        for (int hour = 8; hour < 12; hour++) {
            for (int minute = 0; minute < 60; minute += 15) {
                slots.add(String.format("%02d:%02d", hour, minute));
            }
        }
        // Afternoon slots: 13:00 - 20:00
        for (int hour = 13; hour < 20; hour++) {
            for (int minute = 0; minute < 60; minute += 15) {
                slots.add(String.format("%02d:%02d", hour, minute));
            }
        }
        return slots.toArray(new String[0]);
    }

    private void updatePaymentFields(String method) {
        paypalPanel.setVisible(method.equals("PayPal"));
        creditCardPanel.setVisible(method.equals("Credit Card"));
        invoicePanel.setVisible(method.equals("Invoice"));
    }

    private boolean validateInputs() {
        if (paymentGroup.getSelection() == null) {
            JOptionPane.showMessageDialog(frame, "Please select a payment method");
            return false;
        }

        boolean isPickup = ((JRadioButton)deliveryGroup.getElements().nextElement()).isSelected();
        if (isPickup && timeSlotCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(frame, "Please select a pickup time");
            return false;
        }
        
        if (!isPickup && (streetField.getText().trim().isEmpty() ||
            cityField.getText().trim().isEmpty() ||
            zipField.getText().trim().isEmpty())) {
            JOptionPane.showMessageDialog(frame, "Please fill in all address fields");
            return false;
        }
        
        String selectedMethod = getSelectedPaymentMethod();
        if (selectedMethod == null) {
            JOptionPane.showMessageDialog(frame, "Please select a payment method");
            return false;
        }

        switch (selectedMethod) {
            case "Credit Card":
                if (!validateCreditCard()) return false;
                break;
            case "Invoice":
                if (alternateEmailCheck.isSelected() && !validateEmail(alternateEmailField.getText())) {
                    JOptionPane.showMessageDialog(frame, "Please enter a valid email address");
                    return false;
                }
                break;
        }
        
        return true;
    }

    private boolean validateCreditCard() {
        if (creditCardNumber.getText().length() != 16) {
            JOptionPane.showMessageDialog(frame, "Invalid card number");
            return false;
        }
        if (cvcField.getText().length() != 3) {
            JOptionPane.showMessageDialog(frame, "Invalid CVC");
            return false;
        }
        
        // Validate expiry date
        int month = Integer.parseInt((String) expiryMonth.getSelectedItem());
        int year = Integer.parseInt((String) expiryYear.getSelectedItem());
        LocalDate expiry = LocalDate.of(year, month, 1);
        if (expiry.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(frame, "Card has expired");
            return false;
        }
        
        return true;
    }

    private boolean validateEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private String getSelectedPaymentMethod() {
        ButtonModel selected = paymentGroup.getSelection();
        return selected == null ? null : 
            ((JRadioButton) Arrays.stream(paymentGroup.getElements().nextElement().getParent().getComponents())
                .filter(c -> c instanceof JRadioButton)
                .filter(b -> ((JRadioButton)b).getModel() == selected)
                .findFirst()
                .orElse(null)).getText();
    }

    private void placeOrder() {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String orderQuery = "INSERT INTO orders (user_id, total, status, delivery_address) VALUES (?, ?, ?, ?)";
                int orderId;
                
                String address = String.format("%s, %s %s",
                    streetField.getText().trim(),
                    cityField.getText().trim(),
                    zipField.getText().trim());

                try (PreparedStatement pstmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, user.getId());
                    pstmt.setBigDecimal(2, total);
                    pstmt.setString(3, "Pending");
                    pstmt.setString(4, address);
                    pstmt.executeUpdate();
                    
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            orderId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to get order ID");
                        }
                    }
                }

                String itemQuery = "INSERT INTO order_items (order_id, cake_id, quantity) VALUES (?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(itemQuery)) {
                    for (OrderItem item : items) {
                        pstmt.setInt(1, orderId);
                        pstmt.setInt(2, item.getCake().getId());
                        pstmt.setInt(3, item.getQuantity());
                        pstmt.executeUpdate();
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(frame, "Order placed successfully!");
                frame.dispose();
                new CustomerDashboard(user).display();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error placing order: " + e.getMessage());
        }
    }

    private BigDecimal calculateTotal(Map<Cake, Integer> cart) {
        return cart.entrySet().stream()
            .map(entry -> entry.getKey().getPrice()
                .multiply(BigDecimal.valueOf(entry.getValue())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<OrderItem> createOrderItems(Map<Cake, Integer> cart) {
        List<OrderItem> items = new ArrayList<>();
        cart.forEach((cake, quantity) -> items.add(new OrderItem(cake, quantity)));
        return items;
    }

    private LocalDate[] generateAvailableDays() {
        LocalDate today = LocalDate.now();
        return today.datesUntil(today.plusMonths(1))
            .filter(date -> date.getDayOfWeek().getValue() != 7) // Exclude Sundays
            .toArray(LocalDate[]::new);
    }

    private String[] generateYearRange() {
        int currentYear = LocalDate.now().getYear();
        return IntStream.rangeClosed(currentYear, currentYear + 10)
            .mapToObj(String::valueOf)
            .toArray(String[]::new);
    }

    public void display() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}