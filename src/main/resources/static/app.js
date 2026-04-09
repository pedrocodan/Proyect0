// API Configuration
const API_BASE_URL = "http://localhost:8080/api";
let currentGroupId = null;
let allExpenses = [];
let allMembers = [];

// Initialize
document.addEventListener("DOMContentLoaded", function () {
  loadGroups();
});

// ==================== Groups Management ====================

async function loadGroups() {
  try {
    const response = await fetch(`${API_BASE_URL}/grupos`);
    if (!response.ok) throw new Error("Error loading groups");
    const groups = await response.json();
    updateGroupsList(groups);
    showView("groupsView");
  } catch (error) {
    console.error("Error loading groups:", error);
    document.getElementById("groupsList").innerHTML =
      '<p class="empty-message">No se pudieron cargar los grupos</p>';
    showView("groupsView");
  }
}

function updateGroupsList(groups) {
  const groupsList = document.getElementById("groupsList");

  if (!groups || groups.length === 0) {
    groupsList.innerHTML =
      '<p class="empty-message">No hay grupos creados. Crea uno para comenzar.</p>';
    return;
  }

  groupsList.innerHTML = groups
    .map(
      (group) => `
        <div class="group-card" onclick="openGroup(${group.id})">
          <h3>${group.name}</h3>
          <p>${group.description || "Sin descripción"}</p>
        </div>
      `,
    )
    .join("");
}

async function openGroup(groupId) {
  try {
    const response = await fetch(`${API_BASE_URL}/grupos/${groupId}`);
    if (!response.ok) throw new Error("Error loading group");
    const group = await response.json();
    await loadGroupData(group.id);
    showGroupDashboard(group);
  } catch (error) {
    showAlert("Error al abrir el grupo: " + error.message, "error");
  }
}

function showCreateGroupForm() {
  showView("createGroupView");
}

async function handleCreateGroup(event) {
  event.preventDefault();

  const name = document.getElementById("groupName").value;
  const description = document.getElementById("groupDescription").value;

  try {
    const response = await fetch(`${API_BASE_URL}/grupos`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, description }),
    });

    if (!response.ok) throw new Error("Error creating group");

    const group = await response.json();
    showAlert("Grupo creado exitosamente", "success");

    // Load and show group
    await loadGroupData(group.id);
    showGroupDashboard(group);
  } catch (error) {
    showAlert("Error al crear el grupo: " + error.message, "error");
  }
}

async function loadGroupData(groupId) {
  currentGroupId = groupId;

  try {
    // Load members
    const membersResponse = await fetch(
      `${API_BASE_URL}/grupos/${groupId}/miembros`,
    );
    allMembers = await membersResponse.json();

    // Load expenses
    const expensesResponse = await fetch(
      `${API_BASE_URL}/grupos/${groupId}/gastos`,
    );
    allExpenses = await expensesResponse.json();
  } catch (error) {
    console.error("Error loading group data:", error);
  }
}

function showGroupDashboard(group) {
  currentGroupId = group.id;

  document.getElementById("dashboardGroupName").textContent = group.name;
  document.getElementById("dashboardGroupDescription").textContent =
    group.description || "";

  updateMembersList();
  updateExpensesList();
  updatePaymentsList();
  updateBalancesList();
  updateSelects();

  showView("groupDashboardView");
}

// ==================== Members Management ====================

function showAddMemberForm() {
  document.getElementById("addMemberForm").classList.remove("hidden");
  document.getElementById("memberName").value = "";
  document.getElementById("memberName").focus();
}

function closeAddMemberForm() {
  document.getElementById("addMemberForm").classList.add("hidden");
}

async function handleAddMember(event) {
  event.preventDefault();

  const name = document.getElementById("memberName").value;

  try {
    const response = await fetch(
      `${API_BASE_URL}/grupos/${currentGroupId}/miembros`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name }),
      },
    );

    if (!response.ok) throw new Error("Error adding member");

    const member = await response.json();
    allMembers.push(member);

    showAlert("Miembro agregado exitosamente", "success");
    closeAddMemberForm();
    updateMembersList();
    updateSelects();
  } catch (error) {
    showAlert("Error al agregar miembro: " + error.message, "error");
  }
}

function updateMembersList() {
  const membersList = document.getElementById("membersList");

  if (allMembers.length === 0) {
    membersList.innerHTML = '<p class="empty-message">Sin miembros aún</p>';
    return;
  }

  membersList.innerHTML = allMembers
    .map(
      (member) => `
        <div class="member-card">
            <button class="member-remove" onclick="removeMember(${member.id}, event)">×</button>
            <h4>${member.name}</h4>
        </div>
    `,
    )
    .join("");
}

async function removeMember(memberId, event) {
  event.stopPropagation();

  if (!confirm("¿Eliminar este miembro?")) return;

  try {
    const response = await fetch(
      `${API_BASE_URL}/grupos/${currentGroupId}/miembros/${memberId}`,
      {
        method: "DELETE",
      },
    );

    if (!response.ok) throw new Error("Error removing member");

    allMembers = allMembers.filter((m) => m.id !== memberId);

    showAlert("Miembro eliminado", "success");
    updateMembersList();
    updateSelects();
  } catch (error) {
    showAlert("Error al eliminar miembro: " + error.message, "error");
  }
}

function updateSelects() {
  // Update expense paid by select
  const paidBySelect = document.getElementById("expensePaidBy");
  const debtorSelect = document.getElementById("paymentDebtor");
  const creditorSelect = document.getElementById("paymentCreditor");

  const memberOptions = allMembers
    .map((m) => `<option value="${m.id}">${m.name}</option>`)
    .join("");

  paidBySelect.innerHTML = memberOptions;
  debtorSelect.innerHTML = memberOptions;
  creditorSelect.innerHTML = memberOptions;

  // Update divisions container
  updateDivisionsContainer();
}

// ==================== Expenses Management ====================

function showCreateExpenseForm() {
  if (allMembers.length === 0) {
    showAlert("Debes crear miembros primero", "warning");
    return;
  }

  document.getElementById("createExpenseForm").classList.remove("hidden");
  document.getElementById("expenseAmount").value = "";
  document.getElementById("expenseDescription").value = "";
  document.getElementById("expenseCategory").value = "FOOD";
  updateDivisionsContainer();
}

function closeCreateExpenseForm() {
  document.getElementById("createExpenseForm").classList.add("hidden");
}

function updateDivisionsContainer() {
  const container = document.getElementById("divisionsContainer");
  const amount =
    parseFloat(document.getElementById("expenseAmount").value) || 0;

  let numMembers = allMembers.length;
  let amounts = [];
  if (numMembers > 0 && amount > 0) {
    let totalCents = Math.round(amount * 100);
    let baseCents = Math.floor(totalCents / numMembers);
    let remainderCents = totalCents % numMembers;
    for (let i = 0; i < numMembers; i++) {
      let cents = baseCents + (i < remainderCents ? 1 : 0);
      amounts.push((cents / 100).toFixed(2));
    }
  } else {
    amounts = new Array(numMembers).fill("0.00");
  }

  container.innerHTML = allMembers
    .map(
      (member, index) => `
        <div class="division-input-group">
            <select disabled>
                <option>${member.name}</option>
            </select>
            <input type="number" 
                   class="division-amount" 
                   step="0.01" 
                   min="0" 
                   max="${amount}" 
                   value="${amounts[index]}"
                   data-member-id="${member.id}"
                   onchange="updateDivisionsTotal()"
                   placeholder="0.00">
            <button type="button" class="btn btn-danger btn-small" 
                    onclick="clearDivision(${index})">×</button>
        </div>
    `,
    )
    .join("");

  updateDivisionsTotal();
}

function clearDivision(index) {
  const input = document.querySelectorAll(".division-amount")[index];
  if (input) input.value = "";
  updateDivisionsTotal();
}

function updateDivisionsTotal() {
  const total = Array.from(
    document.querySelectorAll(".division-amount"),
  ).reduce((sum, input) => sum + (parseFloat(input.value) || 0), 0);

  const expenseAmount =
    parseFloat(document.getElementById("expenseAmount").value) || 0;

  document.getElementById("divisionsTotal").textContent = total.toFixed(2);
  document.getElementById("totalAmount").textContent = expenseAmount.toFixed(2);
}

async function handleCreateExpense(event) {
  event.preventDefault();

  const paidById = parseInt(document.getElementById("expensePaidBy").value);
  const amount = parseFloat(document.getElementById("expenseAmount").value);
  const description = document.getElementById("expenseDescription").value;
  const category = document.getElementById("expenseCategory").value;

  // Get divisions
  const divisions = Array.from(document.querySelectorAll(".division-amount"))
    .map((input) => ({
      memberId: parseInt(input.dataset.memberId),
      amount: parseFloat(input.value) || 0,
    }))
    .filter((d) => d.amount > 0);

  if (divisions.length === 0) {
    showAlert("Debes asignar divisiones a los miembros", "warning");
    return;
  }

  const divisionsSum = divisions.reduce((sum, d) => sum + d.amount, 0);
  if (Math.abs(divisionsSum - amount) > 0.01) {
    showAlert("Las divisiones deben sumar exactamente el monto total", "error");
    return;
  }

  try {
    const response = await fetch(
      `${API_BASE_URL}/grupos/${currentGroupId}/gastos`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          paidById,
          amount,
          description,
          category,
          divisions,
        }),
      },
    );

    if (!response.ok) throw new Error("Error creating expense");

    const expense = await response.json();
    allExpenses.push(expense);

    showAlert("Gasto registrado exitosamente", "success");
    closeCreateExpenseForm();
    updateExpensesList();
    updateBalancesList();
    updatePaymentsList();
  } catch (error) {
    showAlert("Error al crear gasto: " + error.message, "error");
  }
}

function updateExpensesList() {
  const expensesList = document.getElementById("expensesList");

  if (allExpenses.length === 0) {
    expensesList.innerHTML =
      '<p class="empty-message">Sin gastos registrados</p>';
    return;
  }

  expensesList.innerHTML = allExpenses
    .map((expense) => {
      const divisionsHtml = (expense.expenseDetails || [])
        .map(
          (detail) => `
            <div class="division-item">
                <span>${detail.memberName}:</span>
                <strong>$${detail.amount.toFixed(2)}</strong>
            </div>
        `,
        )
        .join("");

      return `
            <div class="expense-item">
                <div class="expense-header">
                    <h4>${expense.description}</h4>
                    <span class="expense-category">${getCategoryLabel(expense.category)}</span>
                </div>
                <div class="expense-details">
                    <p><strong>${expense.paidByName}</strong> pagó el <strong class="expense-amount">$${expense.amount.toFixed(2)}</strong></p>
                    <p><small>${new Date(expense.createdAt).toLocaleDateString("es-ES")}</small></p>
                </div>
                <div class="expense-divisions">
                    ${divisionsHtml}
                </div>
            </div>
        `;
    })
    .join("");
}

// ==================== Balances & Settlements ====================

async function updateBalancesList() {
  const balancesList = document.getElementById("balancesList");

  try {
    const response = await fetch(
      `${API_BASE_URL}/grupos/${currentGroupId}/balances`,
    );
    const balances = await response.json();

    if (balances.length === 0) {
      balancesList.innerHTML =
        '<p class="empty-message">Sin deudas pendientes</p>';
      return;
    }

    balancesList.innerHTML = balances
      .map(
        (balance) => `
            <div class="balance-item ${balance.amount > 0 ? "owe" : "debt"}">
                <div class="balance-info">
                    <p><strong>${balance.debtorName}</strong> debe a <strong>${balance.creditorName}</strong></p>
                </div>
                <div class="balance-amount">$${Math.abs(balance.amount).toFixed(2)}</div>
            </div>
        `,
      )
      .join("");
  } catch (error) {
    console.error("Error loading balances:", error);
    balancesList.innerHTML =
      '<p class="empty-message">Error al cargar saldos</p>';
  }
}

// ==================== Payments Management ====================

async function handleRecordPayment(event) {
  event.preventDefault();

  const debtorId = parseInt(document.getElementById("paymentDebtor").value);
  const creditorId = parseInt(document.getElementById("paymentCreditor").value);
  const amount = parseFloat(document.getElementById("paymentAmount").value);
  const description = document.getElementById("paymentDescription").value;

  if (debtorId === creditorId) {
    showAlert("El deudor y acreedor no pueden ser la misma persona", "warning");
    return;
  }

  if (amount <= 0) {
    showAlert("El monto debe ser mayor a 0", "warning");
    return;
  }

  try {
    const response = await fetch(
      `${API_BASE_URL}/grupos/${currentGroupId}/pagos`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          debtorId,
          creditorId,
          amount,
          description,
        }),
      },
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || "Error recording payment");
    }

    const payment = await response.json();

    showAlert("Pago registrado exitosamente", "success");
    document.getElementById("paymentForm").reset();
    updatePaymentsList();
    updateBalancesList();
  } catch (error) {
    showAlert("Error al registrar pago: " + error.message, "error");
  }
}

async function updatePaymentsList() {
  const paymentsList = document.getElementById("paymentsList");

  try {
    const response = await fetch(
      `${API_BASE_URL}/grupos/${currentGroupId}/pagos`,
    );
    const payments = await response.json();

    if (payments.length === 0) {
      paymentsList.innerHTML =
        '<p class="empty-message">Sin pagos registrados</p>';
      return;
    }

    paymentsList.innerHTML = payments
      .map(
        (payment) => `
            <div class="payment-item">
                <div class="payment-info">
                    <p><strong>${payment.debtorName}</strong> pagó a <strong>${payment.creditorName}</strong></p>
                    <p><small>${new Date(payment.createdAt).toLocaleDateString("es-ES")}</small></p>
                    ${payment.description ? `<p><small>${payment.description}</small></p>` : ""}
                </div>
                <div class="payment-amount">$${payment.amount.toFixed(2)}</div>
            </div>
        `,
      )
      .join("");
  } catch (error) {
    console.error("Error loading payments:", error);
    paymentsList.innerHTML =
      '<p class="empty-message">Error al cargar pagos</p>';
  }
}

// ==================== Utilities ====================

function showView(viewId) {
  // Hide all views
  document.querySelectorAll(".view").forEach((view) => {
    view.classList.remove("active");
  });

  // Show selected view
  document.getElementById(viewId).classList.add("active");
}

function showAlert(message, type = "info") {
  const alert = document.createElement("div");
  alert.className = `alert alert-${type}`;
  alert.textContent = message;

  const mainContent = document.querySelector(".main-content");
  mainContent.insertBefore(alert, mainContent.firstChild);

  setTimeout(() => alert.remove(), 4000);
}

function handleModalClick(event) {
  if (event.target === event.currentTarget) {
    event.currentTarget.classList.add("hidden");
  }
}

function getCategoryLabel(category) {
  const labels = {
    FOOD: "🍽️ Comida",
    TRANSPORT: "🚗 Transporte",
    ACCOMMODATION: "🏨 Hospedaje",
    ENTERTAINMENT: "🎭 Entretenimiento",
    OTHER: "📌 Otro",
  };
  return labels[category] || category;
}

// Listen for amount change to update divisions
document.addEventListener("change", function (event) {
  if (event.target.id === "expenseAmount") {
    updateDivisionsContainer();
  }
});
