const state = { q: '', sort: 'relevance', page: 0, size: 20, filters: { city: [], workMode: [], seniority: [], skill: [] } };
const facetTitles = { city: 'Şehir', workMode: 'Çalışma şekli', seniority: 'Kıdem', skill: 'Beceri' };
const labels = {
  ONSITE: 'Ofiste', HYBRID: 'Hibrit', REMOTE: 'Uzaktan',
  JUNIOR: 'Junior', MID: 'Orta', SENIOR: 'Kıdemli', LEAD: 'Takım lideri'
};

const $ = (id) => document.getElementById(id);

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function highlighted(text) {
  return escapeHtml(text).replaceAll('&lt;em&gt;', '<em>').replaceAll('&lt;/em&gt;', '</em>');
}

function salary(job) {
  if (job.salaryMin == null) return '';
  const fmt = (n) => n.toLocaleString('tr-TR');
  return ` · ${fmt(job.salaryMin)} - ${fmt(job.salaryMax)} TL`;
}

function params() {
  const p = new URLSearchParams({ q: state.q, sort: state.sort, page: state.page, size: state.size });
  for (const [name, values] of Object.entries(state.filters)) {
    values.forEach((v) => p.append(name, v));
  }
  return p;
}

async function search() {
  const response = await fetch(`/api/search?${params()}`);
  const result = await response.json();
  if (!response.ok) {
    $('summary').textContent = result.detail || 'Arama yapılamadı';
    return;
  }
  renderResults(result);
  renderFacets(result.facets);
}

function renderResults(result) {
  $('summary').textContent = `${result.total.toLocaleString('tr-TR')} açık ilan`;
  $('results').innerHTML = result.hits.map((job) => {
    const title = job.highlights.title ? highlighted(job.highlights.title[0]) : escapeHtml(job.title);
    const snippet = job.highlights.description ? `<p class="snippet">…${highlighted(job.highlights.description[0])}…</p>` : '';
    const skills = job.skills.map((s) => `<span>${escapeHtml(s)}</span>`).join('');
    return `<li>
      <h3>${title}</h3>
      <div class="meta">${escapeHtml(job.companyName || '')} · ${escapeHtml(job.city)} · ${labels[job.workMode]} · ${labels[job.seniority]}${salary(job)}</div>
      ${snippet}
      <div class="skills">${skills}</div>
    </li>`;
  }).join('');
  const pages = Math.max(1, Math.ceil(Math.min(result.total, 10000) / state.size));
  $('page').textContent = `${state.page + 1} / ${pages}`;
  $('prev').disabled = state.page === 0;
  $('next').disabled = state.page + 1 >= pages;
}

function renderFacets(facets) {
  $('facets').innerHTML = Object.entries(facets).map(([name, values]) => {
    const selected = state.filters[name];
    const items = values.map((v) => `<label>
        <input type="checkbox" data-facet="${name}" value="${escapeHtml(v.value)}" ${selected.includes(v.value) ? 'checked' : ''}>
        ${escapeHtml(labels[v.value] || v.value)}<span class="count">${v.count}</span>
      </label>`).join('');
    return `<h2>${facetTitles[name]}</h2>${items}`;
  }).join('');
}

let suggestTimer;
async function suggest(text) {
  const list = $('suggestions');
  if (text.trim().length < 2) {
    list.hidden = true;
    return;
  }
  const response = await fetch(`/api/suggest?${new URLSearchParams({ q: text })}`);
  const titles = response.ok ? await response.json() : [];
  list.innerHTML = titles.map((t) => `<li role="option">${escapeHtml(t)}</li>`).join('');
  list.hidden = titles.length === 0;
}

$('q').addEventListener('input', (e) => {
  clearTimeout(suggestTimer);
  suggestTimer = setTimeout(() => {
    suggest(e.target.value);
    state.q = e.target.value;
    state.page = 0;
    search();
  }, 200);
});

$('suggestions').addEventListener('click', (e) => {
  if (e.target.tagName !== 'LI') return;
  $('q').value = e.target.textContent;
  $('suggestions').hidden = true;
  state.q = e.target.textContent;
  state.page = 0;
  search();
});

document.addEventListener('click', (e) => {
  if (!e.target.closest('.box')) $('suggestions').hidden = true;
});

$('search-form').addEventListener('submit', (e) => {
  e.preventDefault();
  $('suggestions').hidden = true;
  state.q = $('q').value;
  state.page = 0;
  search();
});

$('sort').addEventListener('change', (e) => {
  state.sort = e.target.value;
  state.page = 0;
  search();
});

$('facets').addEventListener('change', (e) => {
  const { facet } = e.target.dataset;
  if (!facet) return;
  const values = state.filters[facet];
  state.filters[facet] = e.target.checked ? [...values, e.target.value] : values.filter((v) => v !== e.target.value);
  state.page = 0;
  search();
});

$('prev').addEventListener('click', () => { state.page -= 1; search(); window.scrollTo(0, 0); });
$('next').addEventListener('click', () => { state.page += 1; search(); window.scrollTo(0, 0); });

search();
