-- ============================================================================
-- Expense Sharing Application Schema
-- ============================================================================
-- This schema defines all tables, columns, and relationships for the 
-- expense-sharing/settlement application.
-- ============================================================================

-- Groups table: Represents expense-sharing groups
CREATE TABLE groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique group identifier',
    name VARCHAR(255) NOT NULL COMMENT 'Group name',
    description TEXT COMMENT 'Group description',
    created_at TIMESTAMP NOT NULL COMMENT 'Timestamp when the group was created'
);

-- Members table: Represents members within groups
CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique member identifier',
    name VARCHAR(255) NOT NULL COMMENT 'Member name',
    group_id BIGINT NOT NULL COMMENT 'Reference to the group this member belongs to',
    CONSTRAINT fk_members_groups FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);

-- Expenses table: Represents expenses created in a group
CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique expense identifier',
    group_id BIGINT NOT NULL COMMENT 'Reference to the group',
    paid_by_id BIGINT NOT NULL COMMENT 'Reference to the member who paid',
    amount DECIMAL(10,2) NOT NULL COMMENT 'Total expense amount',
    description VARCHAR(255) NOT NULL COMMENT 'Expense description',
    category VARCHAR(50) NOT NULL COMMENT 'Expense category (FOOD, TRANSPORT, ACCOMMODATION, ENTERTAINMENT, OTHER)',
    created_at TIMESTAMP NOT NULL COMMENT 'Timestamp when the expense was created',
    CONSTRAINT fk_expenses_groups FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_members FOREIGN KEY (paid_by_id) REFERENCES members(id) ON DELETE RESTRICT
);

-- Expense Details table: Represents how an expense is divided among members
CREATE TABLE expense_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique expense detail identifier',
    expense_id BIGINT NOT NULL COMMENT 'Reference to the expense',
    member_id BIGINT NOT NULL COMMENT 'Reference to the member who owes part of this expense',
    amount DECIMAL(10,2) NOT NULL COMMENT 'Amount owed by this member for this expense',
    CONSTRAINT fk_expense_details_expenses FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_details_members FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT,
    CONSTRAINT uk_expense_details_unique UNIQUE (expense_id, member_id)
);

-- Payments table: Represents settlement payments between members
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique payment identifier',
    group_id BIGINT NOT NULL COMMENT 'Reference to the group',
    debtor_id BIGINT NOT NULL COMMENT 'Reference to the member who owes money',
    creditor_id BIGINT NOT NULL COMMENT 'Reference to the member who is owed money',
    amount DECIMAL(10,2) NOT NULL COMMENT 'Payment amount',
    description TEXT COMMENT 'Payment description',
    created_at TIMESTAMP NOT NULL COMMENT 'Timestamp when the payment was created',
    CONSTRAINT fk_payments_groups FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_debtor FOREIGN KEY (debtor_id) REFERENCES members(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_creditor FOREIGN KEY (creditor_id) REFERENCES members(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payments_different_members CHECK (debtor_id != creditor_id)
);

-- ============================================================================
-- Create Indexes for Better Query Performance
-- ============================================================================

-- Indexes for groups
CREATE INDEX idx_groups_created_at ON groups(created_at);

-- Indexes for members
CREATE INDEX idx_members_group_id ON members(group_id);
CREATE INDEX idx_members_name ON members(name);

-- Indexes for expenses
CREATE INDEX idx_expenses_group_id ON expenses(group_id);
CREATE INDEX idx_expenses_paid_by_id ON expenses(paid_by_id);
CREATE INDEX idx_expenses_created_at ON expenses(created_at);
CREATE INDEX idx_expenses_category ON expenses(category);

-- Indexes for expense_details
CREATE INDEX idx_expense_details_expense_id ON expense_details(expense_id);
CREATE INDEX idx_expense_details_member_id ON expense_details(member_id);

-- Indexes for payments
CREATE INDEX idx_payments_group_id ON payments(group_id);
CREATE INDEX idx_payments_debtor_id ON payments(debtor_id);
CREATE INDEX idx_payments_creditor_id ON payments(creditor_id);
CREATE INDEX idx_payments_created_at ON payments(created_at);