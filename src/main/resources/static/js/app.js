/**
 * Kaasu Creator - Frontend JavaScript
 */

// Helper function to show temporary alerts
function showAlert(message, type = 'success') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;

    const container = document.querySelector('.container');
    container.insertBefore(alertDiv, container.firstChild);

    setTimeout(() => alertDiv.remove(), 4000);
}

// Escape user/AI-supplied text before inserting it with innerHTML
function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// ===== BUDGET PAGE =====
// Pie chart for expense categories
const PIE_COLORS = [
    '#e879a0', '#d45f88', '#f4a0c0',
    '#9b59b6', '#c0392b', '#e67e22',
    '#27ae60', '#3498db', '#f39c12'
];

function renderPieChart(expenses) {
    const canvas = document.getElementById('pieChart');
    if (!canvas) return;

    const totals = {};
    expenses.forEach(e => {
        totals[e.category] = (totals[e.category] || 0) + e.amount;
    });

    const entries = Object.entries(totals);
    const grand   = entries.reduce((s, [, v]) => s + v, 0);
    if (grand === 0) return;

    const ctx = canvas.getContext('2d');
    const cx  = canvas.width  / 2;
    const cy  = canvas.height / 2;
    const r   = Math.min(cx, cy) - 16;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    let startAngle = -Math.PI / 2;
    entries.forEach(([, amt], i) => {
        const slice = (amt / grand) * 2 * Math.PI;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, r, startAngle, startAngle + slice);
        ctx.closePath();
        ctx.fillStyle   = PIE_COLORS[i % PIE_COLORS.length];
        ctx.strokeStyle = '#080808';
        ctx.lineWidth   = 2;
        ctx.fill();
        ctx.stroke();
        startAngle += slice;
    });

    // Center hole for donut effect
    ctx.beginPath();
    ctx.arc(cx, cy, r * 0.48, 0, 2 * Math.PI);
    ctx.fillStyle = '#111111';
    ctx.fill();

    // Center label
    ctx.fillStyle    = '#f0f0f0';
    ctx.font         = 'bold 13px Segoe UI, sans-serif';
    ctx.textAlign    = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(entries.length + ' categories', cx, cy);

    // Build legend
    const legend = document.getElementById('pie-legend');
    if (!legend) return;
    legend.innerHTML = entries.map(([cat, amt], i) => `
        <div class="b-legend-row">
            <span class="b-legend-dot" style="background:${PIE_COLORS[i % PIE_COLORS.length]}"></span>
            <span class="b-legend-cat">${escapeHtml(cat)}</span>
            <span class="b-legend-pct">${((amt / grand) * 100).toFixed(0)}%</span>
            <span class="b-legend-amt">$${amt.toFixed(2)}</span>
        </div>
    `).join('');
}

// ===== INCOME PAGE =====
// Line chart plotting monthly income totals (custom income + timesheet earnings)
// across every month/year present in the user's history.
const MONTH_NAMES = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

function renderIncomeLineChart(customIncomes, timesheetEntries) {
    const canvas = document.getElementById('incomeLineChart');
    if (!canvas) return;

    // Bucket both data sources into "YYYY-MM" -> total
    const customByMonth = {};
    (customIncomes || []).forEach(inc => {
        if (!inc.incomeDate) return;
        const key = inc.incomeDate.slice(0, 7); // "YYYY-MM"
        customByMonth[key] = (customByMonth[key] || 0) + Number(inc.amount);
    });

    const timesheetByMonth = {};
    (timesheetEntries || []).forEach(e => {
        if (!e.workDate || e.earnedAmount == null) return;
        const key = e.workDate.slice(0, 7);
        timesheetByMonth[key] = (timesheetByMonth[key] || 0) + Number(e.earnedAmount);
    });

    // Union of all months present, sorted chronologically (oldest -> newest)
    const allMonths = Array.from(new Set([...Object.keys(customByMonth), ...Object.keys(timesheetByMonth)])).sort();
    if (allMonths.length === 0) return;

    const customSeries    = allMonths.map(m => customByMonth[m]    || 0);
    const timesheetSeries = allMonths.map(m => timesheetByMonth[m] || 0);
    const labels = allMonths.map(m => {
        const [y, mo] = m.split('-');
        return MONTH_NAMES[parseInt(mo, 10) - 1] + " '" + y.slice(2);
    });

    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);

    const padLeft = 65, padRight = 20, padTop = 20, padBottom = 50;
    const chartW = width - padLeft - padRight;
    const chartH = height - padTop - padBottom;

    const maxVal = Math.max(1, ...customSeries, ...timesheetSeries);
    const niceMax = Math.ceil(maxVal / 5) * 5 || maxVal;

    // Y-axis gridlines + $ labels
    const ySteps = 5;
    ctx.font = '11px Segoe UI, sans-serif';
    ctx.textAlign = 'right';
    ctx.textBaseline = 'middle';
    for (let i = 0; i <= ySteps; i++) {
        const val = (niceMax / ySteps) * i;
        const y = padTop + chartH - (chartH * (i / ySteps));
        ctx.strokeStyle = '#2a2a2a';
        ctx.beginPath();
        ctx.moveTo(padLeft, y);
        ctx.lineTo(padLeft + chartW, y);
        ctx.lineWidth = 1;
        ctx.stroke();
        ctx.fillStyle = '#999';
        ctx.fillText('$' + val.toFixed(0), padLeft - 8, y);
    }

    // X axis month labels
    const n = allMonths.length;
    const stepX = n > 1 ? chartW / (n - 1) : 0;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    labels.forEach((label, i) => {
        const x = padLeft + (n > 1 ? stepX * i : chartW / 2);
        ctx.fillStyle = '#999';
        ctx.fillText(label, x, padTop + chartH + 10);
    });

    // Axes (crossing at the origin, bottom-left of the plot area)
    ctx.strokeStyle = '#555';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(padLeft, padTop);
    ctx.lineTo(padLeft, padTop + chartH);
    ctx.lineTo(padLeft + chartW, padTop + chartH);
    ctx.stroke();

    // Plot a single series as a connected line with point markers
    function plotSeries(series, color) {
        if (n === 0) return;
        ctx.strokeStyle = color;
        ctx.lineWidth = 2.5;
        ctx.beginPath();
        series.forEach((val, i) => {
            const x = padLeft + (n > 1 ? stepX * i : chartW / 2);
            const y = padTop + chartH - (val / niceMax) * chartH;
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke();

        ctx.fillStyle = color;
        series.forEach((val, i) => {
            const x = padLeft + (n > 1 ? stepX * i : chartW / 2);
            const y = padTop + chartH - (val / niceMax) * chartH;
            ctx.beginPath();
            ctx.arc(x, y, 4, 0, 2 * Math.PI);
            ctx.fill();
        });
    }

    plotSeries(timesheetSeries, PIE_COLORS[1]); // timesheet earnings
    plotSeries(customSeries, PIE_COLORS[0]);    // custom income

    // Legend
    const legend = document.getElementById('income-line-legend');
    if (!legend) return;
    const customTotal    = customSeries.reduce((s, v) => s + v, 0);
    const timesheetTotal = timesheetSeries.reduce((s, v) => s + v, 0);
    legend.innerHTML = `
        <div class="ic-legend-row">
            <span class="ic-legend-dot" style="background:${PIE_COLORS[0]}"></span>
            <span>Custom Income: $${customTotal.toFixed(2)}</span>
        </div>
        <div class="ic-legend-row">
            <span class="ic-legend-dot" style="background:${PIE_COLORS[1]}"></span>
            <span>Timesheet Earnings: $${timesheetTotal.toFixed(2)}</span>
        </div>
    `;
}

// ===== GOAL PAGE =====
// Calculate and display progress for goals
function updateGoalProgress(goalId, targetAmount, currentAmount) {
    const progress = Math.min((currentAmount / targetAmount) * 100, 100);
    const progressBar = document.querySelector(`#goal-${goalId} .progress-fill`);
    const progressText = document.querySelector(`#goal-${goalId} .progress-text`);

    if (progressBar) {
        progressBar.style.width = `${progress}%`;
    }
    if (progressText) {
        progressText.textContent = `$${currentAmount.toFixed(2)} / $${targetAmount.toFixed(2)} (${progress.toFixed(1)}%)`;
    }
}

// ===== KAASU-MASCOT INTERACTION =====
// Toggle speech bubble
document.addEventListener('DOMContentLoaded', () => {
    const mascot = document.querySelector('.kaasu-mascot');
    if (mascot) {
        mascot.addEventListener('click', () => {
            const speech = mascot.querySelector('.kaasu-speech');
            speech.style.display = speech.style.display === 'block' ? 'none' : 'block';
        });
    }

    // For forms, add confirmation before submit
    const forms = document.querySelectorAll('form[data-confirm]');
    forms.forEach(form => {
        form.addEventListener('submit', (e) => {
            if (!confirm(form.dataset.confirm)) {
                e.preventDefault();
            }
        });
    });
});

// ===== EXPENSE DELETION =====
// Confirm before deleting
function confirmDelete(expenseId) {
    if (confirm('Delete this expense? This cannot be undone.')) {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/budget/delete';

        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'expenseId';
        input.value = expenseId;
        form.appendChild(input);

        const csrfMeta = document.querySelector('meta[name="_csrf"]');
        if (csrfMeta) {
            const csrf = document.createElement('input');
            csrf.type = 'hidden';
            csrf.name = '_csrf';
            csrf.value = csrfMeta.content;
            form.appendChild(csrf);
        }

        document.body.appendChild(form);
        form.submit();
    }
}

// ===== ROADMAP CHECKBOX =====
function markWeekComplete(roadmapId, checked) {
    // This could be an AJAX call to update status
    console.log(`Week ${roadmapId} marked as ${checked ? 'complete' : 'pending'}`);
}