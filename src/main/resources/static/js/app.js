(function () {
    'use strict';

    const API = {
        login: '/login/auth',
        refresh: '/login/refresh',
        logout: '/login/logout',
        units: '/units',
        users: '/users',
        roles: '/roles',
        permissions: '/permissions',
    };

    const ACCESS_TOKEN_KEY = 'auth_token';
    const REFRESH_TOKEN_KEY = 'refresh_token';
    let refreshInFlight = null;

    const SYSTEM_DEFINED_USERS = new Set(['admin']);
    const SYSTEM_DEFINED_ROLES = new Set(['Полные права']);

    const sections = {
        home: { id: 'home', title: 'Начальная страница', tabLabel: 'Начальная страница', tabIcon: '🏠', pinned: true },
        warehouse: { id: 'warehouse', title: 'Склад', tabLabel: 'Склад', tabIcon: '📦', sidebarLabel: 'Склад', sidebarIcon: '📦' },
        nsiAdmin: { id: 'nsiAdmin', title: 'НСИ и Администрирование', tabLabel: 'НСИ и Администрирование', tabIcon: '📋', sidebarLabel: 'НСИ и Администрирование', sidebarIcon: '⚙️' },
        adminSettings: { id: 'adminSettings', title: 'Настройки пользователей и прав', tabLabel: 'Настройки', tabIcon: '🔐', parent: 'nsiAdmin' },
        units: { id: 'units', title: 'Единицы измерения', tabLabel: 'Единицы измерения', tabIcon: '📏', parent: 'nsiAdmin' },
        users: { id: 'users', title: 'Пользователи', tabLabel: 'Пользователи', tabIcon: '👤', parent: 'adminSettings' },
        roles: { id: 'roles', title: 'Роли пользователей', tabLabel: 'Роли', tabIcon: '🛡', parent: 'adminSettings' },
        permissions: { id: 'permissions', title: 'Права пользователей', tabLabel: 'Права', tabIcon: '🔑', parent: 'adminSettings', readOnly: true },
        unitCard: { id: 'unitCard', title: 'Единица измерения', viewId: 'unitCard' },
        userCard: { id: 'userCard', title: 'Пользователь', viewId: 'userCard' },
        roleCard: { id: 'roleCard', title: 'Роль', viewId: 'roleCard' },
        permissionCard: { id: 'permissionCard', title: 'Право', viewId: 'permissionCard' },
    };

    const entityMeta = {
        units: { api: API.units, listKey: 'units', listSection: 'units', cardSection: 'unitCard', hash: 'units', label: (i) => i.name || 'Единица' },
        users: { api: API.users, listKey: 'users', listSection: 'users', cardSection: 'userCard', hash: 'users', label: (i) => i.username || 'Пользователь' },
        roles: { api: API.roles, listKey: 'roles', listSection: 'roles', cardSection: 'roleCard', hash: 'roles', label: (i) => i.name || 'Роль' },
        permissions: { api: API.permissions, listKey: 'permissions', listSection: 'permissions', cardSection: 'permissionCard', hash: 'permissions', label: (i) => i.name || 'Право', readOnly: true },
    };

    const ENTITY_PERMISSIONS = {
        units: { read: ['UNITS_READ', 'UNITS_WRITE'], write: ['UNITS_WRITE'] },
        users: { read: ['USERS_READ', 'USERS_WRITE'], write: ['USERS_WRITE'] },
        roles: { read: ['ROLES_READ', 'ROLES_WRITE'], write: ['ROLES_WRITE'] },
        permissions: { read: ['ROLES_READ', 'ROLES_WRITE'], write: [] },
    };

    let userPermissionCodes = new Set();
    let currentUsername = '';

    const searchIndex = [
        { title: 'Начальная страница', keywords: ['начальная', 'главная'], path: 'Главное', sectionId: 'home' },
        { title: 'Склад', keywords: ['склад'], path: 'Подсистемы', sectionId: 'warehouse' },
        { title: 'НСИ и Администрирование', keywords: ['нси', 'администрирование'], path: 'НСИ', sectionId: 'nsiAdmin' },
        { title: 'Настройки пользователей и прав', keywords: ['настройки', 'пользователи', 'права', 'роли'], path: 'Администрирование', sectionId: 'adminSettings' },
        { title: 'Единицы измерения', keywords: ['единицы', 'измерение'], path: 'НСИ', sectionId: 'units', entity: 'units' },
        { title: 'Пользователи', keywords: ['пользователь', 'логин', 'admin'], path: 'Администрирование', sectionId: 'users', entity: 'users' },
        { title: 'Роли пользователей', keywords: ['роль', 'роли'], path: 'Администрирование', sectionId: 'roles', entity: 'roles' },
        { title: 'Права пользователей', keywords: ['право', 'права', 'доступ'], path: 'Администрирование', sectionId: 'permissions', entity: 'permissions' },
    ];

    let openTabs = [{ id: 'home', sectionId: 'home', pinned: true }];
    let activeTabId = 'home';
    let skipHashSync = false;
    let toastTimer = null;
    let validationQueue = [];
    let validationIndex = 0;
    let activeCardEntity = null;
    let activeCardBody = null;

    const listState = {
        units: { cache: [], selectedId: null },
        users: { cache: [], selectedId: null },
        roles: { cache: [], selectedId: null },
        permissions: { cache: [], selectedId: null },
    };

    const cardState = {
        units: { deleted: false, systemDefined: false, readonly: false },
        users: { deleted: false, systemDefined: false, readonly: false, usernameManual: false },
        roles: { deleted: false, systemDefined: false, readonly: false },
        permissions: { deleted: false, systemDefined: true, readonly: true },
    };

    const sessionDeleted = { units: new Set(), users: new Set(), roles: new Set() };
    let permissionIdByCode = new Map();
    let rolesCatalog = [];
    let permissionsCatalog = [];

    const els = {
        sidebarNav: document.getElementById('sidebar-nav'),
        tabsBar: document.getElementById('tabs-bar'),
        globalSearch: document.getElementById('global-search'),
        searchDropdown: document.getElementById('search-dropdown'),
        validationBubble: document.getElementById('validation-error-bubble'),
        validationMessage: document.getElementById('validation-error-message'),
        validationNav: document.getElementById('validation-error-nav'),
        validationCounter: document.getElementById('validation-error-counter'),
        validationPrev: document.getElementById('validation-error-prev'),
        validationNext: document.getElementById('validation-error-next'),
        validationClose: document.getElementById('validation-error-close'),
    };

    function hasPermission(...codes) {
        if (userPermissionCodes.has('FULL_ACCESS')) return true;
        return codes.some((code) => userPermissionCodes.has(code));
    }

    function canReadEntity(entity) {
        const perms = ENTITY_PERMISSIONS[entity];
        return perms ? hasPermission(...perms.read) : false;
    }

    function canWriteEntity(entity) {
        const perms = ENTITY_PERMISSIONS[entity];
        return perms ? hasPermission(...perms.write) : false;
    }

    function isEntityReadOnlyByPermission(entity) {
        return !canWriteEntity(entity) && !entityMeta[entity]?.readOnly;
    }

    function canAccessSection(sectionId) {
        switch (sectionId) {
            case 'home':
            case 'warehouse':
                return true;
            case 'nsiAdmin':
                return canReadEntity('units') || canAccessSection('adminSettings');
            case 'adminSettings':
                return canReadEntity('users') || canReadEntity('roles') || canReadEntity('permissions');
            case 'units':
            case 'unitCard':
                return canReadEntity('units');
            case 'users':
            case 'userCard':
                return canReadEntity('users');
            case 'roles':
            case 'roleCard':
                return canReadEntity('roles');
            case 'permissions':
            case 'permissionCard':
                return canReadEntity('permissions');
            default:
                return true;
        }
    }

    function canAccessRoute(route) {
        if (!route) return false;
        if (route.type === 'home' || route.type === 'warehouse') return true;
        if (route.type === 'nsi') return canAccessSection('nsiAdmin');
        if (route.type === 'admin') return canAccessSection('adminSettings');
        if (route.type === 'list' || route.type === 'card') return canReadEntity(route.entity);
        if (route.type === 'cardNew') return canWriteEntity(route.entity);
        return true;
    }

    async function loadCurrentUser() {
        const data = await apiRequest(API.users + '/me');
        userPermissionCodes = new Set(data.permissionCodes || []);
        currentUsername = data.username || '';
        const usernameEl = document.getElementById('header-username');
        if (usernameEl) usernameEl.textContent = currentUsername;
    }

    function applyPermissionUi() {
        document.getElementById('nsi-block-units')?.classList.toggle('hidden', !canReadEntity('units'));
        document.getElementById('nsi-block-admin')?.classList.toggle('hidden', !canAccessSection('adminSettings'));
        document.getElementById('btn-load-units')?.classList.toggle('hidden', !canReadEntity('units'));
        document.getElementById('btn-load-admin-settings')?.classList.toggle('hidden', !canAccessSection('adminSettings'));
        document.getElementById('btn-load-users')?.classList.toggle('hidden', !canReadEntity('users'));
        document.getElementById('btn-load-roles')?.classList.toggle('hidden', !canReadEntity('roles'));
        document.getElementById('btn-load-permissions')?.classList.toggle('hidden', !canReadEntity('permissions'));

        els.sidebarNav?.querySelectorAll('.sidebar__link').forEach((link) => {
            const section = link.dataset.section;
            const visible = section === 'warehouse' || canAccessSection(section);
            link.closest('li')?.classList.toggle('hidden', !visible);
        });

        Object.keys(entityMeta).forEach((entity) => {
            const view = getListView(entity);
            if (!view) return;
            const createBtn = view.querySelector('[data-action="create"]');
            if (createBtn) createBtn.classList.toggle('hidden', !canWriteEntity(entity));
            const editBtn = view.querySelector('[data-action="edit"]');
            if (editBtn) editBtn.textContent = canWriteEntity(entity) ? 'Изменить' : 'Просмотр';
            updateListToolbar(entity);
        });
    }

    function denyAccess(message) {
        showToast(message || 'Недостаточно прав для этого раздела', true);
    }

    function viewEl(sectionId) {
        const map = {
            home: 'view-home',
            warehouse: 'view-warehouse-hub',
            nsiAdmin: 'view-nsi-admin',
            adminSettings: 'view-admin-settings',
            units: 'view-units',
            users: 'view-users',
            roles: 'view-roles',
            permissions: 'view-permissions',
            unitCard: 'view-unit-card',
            userCard: 'view-user-card',
            roleCard: 'view-role-card',
            permissionCard: 'view-permission-card',
        };
        return document.getElementById(map[sectionId]);
    }

    function getListView(entity) {
        return document.getElementById('view-' + entity);
    }

    function getCardView(entity) {
        return document.getElementById('view-' + entity.replace(/s$/, '') + '-card');
    }

    function getCardRoot(entity) {
        const ids = { units: 'unit-card-root', users: 'user-card-root', roles: 'role-card-root', permissions: 'permission-card-root' };
        return document.getElementById(ids[entity]);
    }

    function apiUrl(entity, suffix) {
        const base = entityMeta[entity].api;
        return suffix ? base + '/' + suffix : base + '/';
    }

    function escapeHtml(text) {
        if (text == null) return '';
        const div = document.createElement('div');
        div.textContent = String(text);
        return div.innerHTML;
    }

    function showToast(message, isError) {
        document.querySelectorAll('.toast').forEach((t) => t.remove());
        clearTimeout(toastTimer);
        const t = document.createElement('div');
        t.className = 'toast' + (isError ? ' toast--error' : '');
        t.textContent = message;
        document.body.appendChild(t);
        toastTimer = setTimeout(() => t.remove(), 3500);
    }

    function isDeleted(entity, item) {
        if (item && item.deleted === true) return true;
        const id = item && item.id != null ? Number(item.id) : null;
        return id != null && sessionDeleted[entity] && sessionDeleted[entity].has(id);
    }

    function isSystemDefined(entity, item) {
        if (item && item.systemDefined === true) return true;
        if (entity === 'users' && item && SYSTEM_DEFINED_USERS.has(item.username)) return true;
        if (entity === 'roles' && item && SYSTEM_DEFINED_ROLES.has(item.name)) return true;
        if (entity === 'permissions') return true;
        return false;
    }

    function markSessionDeleted(entity, id, deleted) {
        if (!sessionDeleted[entity]) return;
        const num = Number(id);
        if (deleted) sessionDeleted[entity].add(num);
        else sessionDeleted[entity].delete(num);
    }

    async function readJsonResponse(res) {
        const text = await res.text();
        if (!text) return null;
        return JSON.parse(text);
    }

    function parseApiErrorBody(status, bodyText) {
        if (!bodyText) return { message: 'HTTP ' + status, fieldErrors: [] };
        try {
            const json = JSON.parse(bodyText);
            const fieldErrors = Array.isArray(json.errors)
                ? json.errors.filter((e) => e && e.field && e.message).map((e) => ({ field: e.field, message: e.message }))
                : [];
            if (json.message) return { message: json.message, fieldErrors };
            if (json.error) return { message: json.error + (json.status ? ' (' + json.status + ')' : ''), fieldErrors };
        } catch (e) { /* */ }
        return { message: bodyText.length > 200 ? 'HTTP ' + status : bodyText, fieldErrors: [] };
    }

    function createApiError(status, bodyText) {
        const parsed = parseApiErrorBody(status, bodyText);
        const err = new Error(parsed.message);
        err.status = status;
        err.fieldErrors = parsed.fieldErrors;
        return err;
    }

    function getAccessToken() {
        return localStorage.getItem(ACCESS_TOKEN_KEY);
    }

    function getRefreshToken() {
        return localStorage.getItem(REFRESH_TOKEN_KEY);
    }

    function storeTokens(data) {
        if (data.accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
        if (data.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
    }

    function clearTokens() {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
    }

    function authJsonHeaders() {
        return {
            'Content-Type': 'application/json; charset=UTF-8',
            'X-Requested-With': 'XMLHttpRequest',
        };
    }

    async function refreshSession() {
        if (refreshInFlight) return refreshInFlight;
        refreshInFlight = (async () => {
            const refreshToken = getRefreshToken();
            if (!refreshToken) return false;
            try {
                const res = await fetch(API.refresh, {
                    method: 'POST',
                    headers: authJsonHeaders(),
                    body: JSON.stringify({ refreshToken }),
                });
                if (!res.ok) return false;
                const data = await res.json();
                if (!data.accessToken || !data.refreshToken) return false;
                storeTokens(data);
                return true;
            } catch (e) {
                return false;
            }
        })();
        try {
            return await refreshInFlight;
        } finally {
            refreshInFlight = null;
        }
    }

    async function apiRequest(url, options = {}) {
        const init = {
            method: options.method || 'GET',
            ...options,
            headers: { ...authJsonHeaders(), ...(options.headers || {}) },
        };
        const token = getAccessToken();
        if (token) init.headers['Authorization'] = 'Bearer ' + token;
        if (init.body == null) delete init.body;

        let res = await fetch(url, init);
        if (res.status === 401) {
            const refreshed = await refreshSession();
            if (refreshed) {
                init.headers['Authorization'] = 'Bearer ' + getAccessToken();
                res = await fetch(url, init);
            }
        }
        if (res.status === 401) {
            handleUnauthorized();
            throw createApiError(res.status, 'Необходима авторизация');
        }
        if (!res.ok) throw createApiError(res.status, await res.text());
        if (res.status === 204) return null;
        return readJsonResponse(res);
    }

    async function buildPermissionIdMap() {
        const map = new Map();
        try {
            const data = await apiRequest(apiUrl('permissions'));
            const items = data.permissions || [];
            items.forEach(p => {
                if (p && p.code) map.set(p.code, p.id);
            });
        } catch (e) {
            console.error('Failed to load permissions map', e);
        }
        permissionIdByCode = map;
        return map;
    }

    function generateUsername(lastName, firstName, middleName) {
        const ln = (lastName || '').trim();
        const fn = (firstName || '').trim();
        const mn = (middleName || '').trim();
        let username = ln;
        if (fn) username += fn.charAt(0);
        if (mn) username += mn.charAt(0);
        return username;
    }

    function evaluatePasswordStrength(password) {
        const p = password || '';
        const rules = {
            length: p.length >= 8,
            lower: /[a-zа-яё]/.test(p),
            upper: /[A-ZА-ЯЁ]/.test(p),
            digit: /\d/.test(p),
        };
        const score = Object.values(rules).filter(Boolean).length;
        let level = 'weak';
        if (score >= 4) level = 'strong';
        else if (score >= 3) level = 'good';
        else if (score >= 2) level = 'fair';
        return { rules, level, score };
    }

    function updatePasswordStrengthUI(container, password) {
        if (!container) return;
        const { rules, level } = evaluatePasswordStrength(password);
        container.className = 'password-strength password-strength--' + level;
        const label = container.querySelector('.password-strength__label');
        const labels = { weak: 'Слабый', fair: 'Средний', good: 'Хороший', strong: 'Надёжный' };
        if (label) label.textContent = 'Надёжность: ' + (labels[level] || '');
        const rulesEl = container.closest('.form-field')?.querySelector('[data-role="password-rules"]');
        if (rulesEl) {
            rulesEl.querySelectorAll('[data-rule]').forEach((li) => {
                const key = li.dataset.rule;
                li.classList.toggle('rule--ok', !!rules[key]);
            });
        }
    }

    function getActiveTab() {
        return openTabs.find((t) => t.id === activeTabId);
    }

    function getViewKey(sectionId) {
        const cardSections = ['unitCard', 'userCard', 'roleCard', 'permissionCard'];
        return cardSections.includes(sectionId) ? sectionId : sectionId;
    }

    function showViewForTab(tab) {
        document.querySelectorAll('.view').forEach((el) => el.classList.remove('view--active'));
        const view = viewEl(tab.sectionId);
        if (view) view.classList.add('view--active');
    }

    function updateSidebarActive() {
        const tab = getActiveTab();
        const activeSection = tab ? tab.sectionId : 'home';
        els.sidebarNav.querySelectorAll('.sidebar__link').forEach((link) => {
            const id = link.dataset.section;
            const meta = sections[activeSection];
            const isActive = id === activeSection || (meta && meta.parent === id) ||
                (activeSection.endsWith('Card') && tab && tab.fromSection === id) ||
                (['units', 'users', 'roles', 'permissions', 'adminSettings', 'unitCard', 'userCard', 'roleCard', 'permissionCard'].includes(activeSection) &&
                    ((id === 'nsiAdmin' && ['units', 'unitCard'].includes(activeSection)) ||
                        (id === 'nsiAdmin' && ['adminSettings', 'users', 'roles', 'permissions', 'userCard', 'roleCard', 'permissionCard'].includes(activeSection))));
            link.classList.toggle('sidebar__link--active', isActive);
        });
    }

    function renderTabs() {
        els.tabsBar.innerHTML = '';
        openTabs.forEach((tab) => {
            const meta = sections[tab.sectionId] || {};
            const wrap = document.createElement('div');
            wrap.className = 'app-tab' + (tab.id === activeTabId ? ' app-tab--active' : '') + (tab.pinned ? ' app-tab--pinned' : '');
            const labelBtn = document.createElement('button');
            labelBtn.type = 'button';
            labelBtn.className = 'app-tab__label';
            labelBtn.innerHTML = '<span class="app-tab__icon">' + (tab.icon || meta.tabIcon || '📄') + '</span><span>' + escapeHtml(tab.label || meta.tabLabel || meta.title) + '</span>';
            labelBtn.addEventListener('click', () => switchTab(tab.id));
            wrap.appendChild(labelBtn);
            if (!tab.pinned) {
                const closeBtn = document.createElement('button');
                closeBtn.type = 'button';
                closeBtn.className = 'app-tab__close';
                closeBtn.textContent = '×';
                closeBtn.addEventListener('click', (e) => { e.stopPropagation(); closeTab(tab.id); });
                wrap.appendChild(closeBtn);
            }
            els.tabsBar.appendChild(wrap);
        });
    }

    function openTab(tabDef) {
        const existing = openTabs.find((t) => t.id === tabDef.id);
        if (existing) Object.assign(existing, tabDef);
        else openTabs.push(tabDef);
    }

    function closeTab(tabId) {
        const tab = openTabs.find((t) => t.id === tabId);
        if (!tab || tab.pinned) return;
        const idx = openTabs.findIndex((t) => t.id === tabId);
        openTabs.splice(idx, 1);
        if (activeTabId === tabId) {
            const next = openTabs[Math.min(idx, openTabs.length - 1)] || openTabs[0];
            activeTabId = next.id;
            showViewForTab(next);
        }
        updateSidebarActive();
        renderTabs();
        syncHash();
    }

    function navigateTo(sectionId, options = {}) {
        const meta = sections[sectionId];
        if (!meta) return;
        if (!canAccessSection(sectionId)) {
            denyAccess();
            return;
        }
        const tabId = options.tabId || sectionId;
        openTab({ id: tabId, sectionId, label: options.label || meta.tabLabel, icon: options.icon || meta.tabIcon, pinned: meta.pinned, fromSection: options.fromSection });
        activeTabId = tabId;
        const entity = Object.keys(entityMeta).find((k) => entityMeta[k].listSection === sectionId);
        if (entity && options.load !== false && canReadEntity(entity)) loadEntityList(entity);
        showViewForTab(getActiveTab());
        updateSidebarActive();
        renderTabs();
        if (options.syncHash !== false) syncHash();
    }

    function hashFromTab(tab) {
        if (!tab || tab.sectionId === 'home') return '#/';
        if (tab.sectionId === 'warehouse') return '#/warehouse';
        if (tab.sectionId === 'nsiAdmin') return '#/nsi';
        if (tab.sectionId === 'adminSettings') return '#/admin';
        for (const [entity, meta] of Object.entries(entityMeta)) {
            if (tab.sectionId === meta.listSection) return '#/' + meta.hash;
            if (tab.sectionId === meta.cardSection) {
                return tab.recordId == null ? '#/' + meta.hash + '/new' : '#/' + meta.hash + '/' + tab.recordId;
            }
        }
        return '#/';
    }

    function parseHash(hash) {
        const raw = (hash || location.hash || '#/').replace(/^#/, '') || '/';
        const parts = raw.split('/').filter(Boolean);
        if (!parts.length) return { type: 'home' };
        if (parts[0] === 'warehouse') return { type: 'warehouse' };
        if (parts[0] === 'nsi') return { type: 'nsi' };
        if (parts[0] === 'admin') return { type: 'admin' };
        const entity = Object.keys(entityMeta).find((k) => entityMeta[k].hash === parts[0]);
        if (entity) {
            if (parts.length === 1) return { type: 'list', entity };
            if (parts[1] === 'new') return { type: 'cardNew', entity };
            const id = parseInt(parts[1], 10);
            if (!Number.isNaN(id) && id > 0) return { type: 'card', entity, id };
        }
        return null;
    }

    function routeFromAppState() {
        const tab = getActiveTab();
        if (!tab) return { type: 'home' };
        if (tab.sectionId === 'home') return { type: 'home' };
        if (tab.sectionId === 'warehouse') return { type: 'warehouse' };
        if (tab.sectionId === 'nsiAdmin') return { type: 'nsi' };
        if (tab.sectionId === 'adminSettings') return { type: 'admin' };
        for (const [entity, meta] of Object.entries(entityMeta)) {
            if (tab.sectionId === meta.listSection) return { type: 'list', entity };
            if (tab.sectionId === meta.cardSection) {
                return tab.recordId == null ? { type: 'cardNew', entity } : { type: 'card', entity, id: tab.recordId };
            }
        }
        return { type: 'home' };
    }

    function routesEqual(a, b) {
        if (!a || !b || a.type !== b.type) return false;
        if (a.type === 'card') return a.entity === b.entity && a.id === b.id;
        if (a.type === 'cardNew' || a.type === 'list') return a.entity === b.entity;
        return true;
    }

    function routePath(route) {
        if (route.type === 'home') return '#/';
        if (route.type === 'warehouse') return '#/warehouse';
        if (route.type === 'nsi') return '#/nsi';
        if (route.type === 'admin') return '#/admin';
        if (route.type === 'list') return '#/' + entityMeta[route.entity].hash;
        if (route.type === 'cardNew') return '#/' + entityMeta[route.entity].hash + '/new';
        if (route.type === 'card') return '#/' + entityMeta[route.entity].hash + '/' + route.id;
        return '#/';
    }

    function routeTitle(route) {
        if (route.type === 'home') return 'Начальная страница';
        if (route.type === 'warehouse') return 'Склад';
        if (route.type === 'nsi') return 'НСИ и Администрирование';
        if (route.type === 'admin') return 'Настройки пользователей и прав';
        if (route.type === 'list') return sections[entityMeta[route.entity].listSection]?.title || route.entity;
        if (route.type === 'cardNew') return 'Новая запись';
        if (route.type === 'card') return 'Запись № ' + route.id;
        return 'Переход';
    }

    function syncHash(replace) {
        if (skipHashSync) return;
        const target = hashFromTab(getActiveTab());
        const current = location.hash || '#/';
        if (current === target) return;
        if (replace) history.replaceState(null, '', target);
        else location.hash = target.slice(1);
    }

    async function applyRoute(route) {
        if (!route) return true;
        if (!canAccessRoute(route)) {
            denyAccess();
            switchTab('home');
            syncHash(true);
            return false;
        }
        switch (route.type) {
            case 'home': switchTab('home'); return true;
            case 'warehouse': navigateTo('warehouse', { syncHash: false }); return true;
            case 'nsi': navigateTo('nsiAdmin', { syncHash: false }); return true;
            case 'admin': navigateTo('adminSettings', { syncHash: false }); return true;
            case 'list': navigateTo(entityMeta[route.entity].listSection, { syncHash: false }); return true;
            case 'cardNew': return openEntityCard(route.entity, null, { syncHash: false });
            case 'card': return openEntityCard(route.entity, route.id, { syncHash: false });
            default: return true;
        }
    }

    async function navigateByRoute(route) {
        skipHashSync = true;
        await applyRoute(route);
        skipHashSync = false;
        location.hash = routePath(route).slice(1);
    }

    async function onHashChange() {
        const route = parseHash(location.hash);
        if (!route) {
            if (location.hash) showToast('Ссылка не распознана: ' + location.hash, true);
            return;
        }
        if (routesEqual(route, routeFromAppState())) return;
        skipHashSync = true;
        await applyRoute(route);
        skipHashSync = false;
    }

    async function initRouter() {
        window.addEventListener('hashchange', onHashChange);
        const route = parseHash(location.hash);
        if (!route && location.hash) {
            showToast('Ссылка не распознана: ' + location.hash, true);
            switchTab('home');
            syncHash(true);
            return;
        }
        if (route && (location.hash || route.type !== 'home')) {
            skipHashSync = true;
            await applyRoute(route);
            skipHashSync = false;
        } else {
            switchTab('home');
            syncHash(true);
        }
    }

    async function switchTab(tabId) {
        const tab = openTabs.find((t) => t.id === tabId);
        if (!tab) return;
        activeTabId = tabId;
        activeCardEntity = tab.entity || null;
        if (tab.entity && tab.recordId != null) {
            await loadEntityIntoCard(tab.entity, tab.recordId);
        } else if (tab.entity && tab.recordId == null) {
            clearEntityCard(tab.entity, false);
            const root = getCardRoot(tab.entity);
            root.querySelector('[data-role="header"]').textContent = 'Новая запись';
            if (tab.entity === 'users') {
                root.querySelector('[data-role="create-password-block"]')?.classList.remove('hidden');
                root.querySelector('[data-role="enabled-field"]')?.classList.add('hidden');
                await populateRolesChecklist(root.querySelector('[data-role="roles-checklist"]'), []);
            }
            if (tab.entity === 'roles') {
                await populatePermissionsChecklist(root.querySelector('[data-role="permissions-checklist"]'), [], false);
            }
            updateCardActions(tab.entity);
        }
        showViewForTab(tab);
        updateSidebarActive();
        renderTabs();
        syncHash();
    }

    function setListStatus(entity, message, isError) {
        const view = getListView(entity);
        const el = view?.querySelector('[data-role="status"]');
        if (el) {
            el.textContent = message;
            el.classList.toggle('entity-status--error', !!isError);
        }
    }

    function updateListToolbar(entity) {
        const view = getListView(entity);
        if (!view) return;
        const state = listState[entity];
        const hasSelection = state.selectedId != null;
        const item = state.cache.find((i) => String(i.id) === String(state.selectedId));
        const readOnly = entityMeta[entity].readOnly || (item && isSystemDefined(entity, item));
        view.querySelector('[data-action="edit"]')?.toggleAttribute('disabled', !hasSelection);
        view.querySelector('[data-action="view"]')?.toggleAttribute('disabled', !hasSelection);
        view.querySelector('[data-action="copy-link"]')?.toggleAttribute('disabled', !hasSelection);
        const createBtn = view.querySelector('[data-action="create"]');
        if (createBtn) createBtn.classList.toggle('hidden', !canWriteEntity(entity) || entityMeta[entity].readOnly);
    }

    function renderMarkCell(entity, item) {
        let html = '';
        if (isDeleted(entity, item)) html += '<span class="deleted-badge" title="Помечено на удаление">🗑</span> ';
        if (isSystemDefined(entity, item)) html += '<span class="system-badge" title="Предопределённый">⚙</span>';
        return html;
    }

    function renderEntityTable(entity, items) {
        const view = getListView(entity);
        const state = listState[entity];
        state.cache = items || [];
        const tbody = view.querySelector('[data-role="table-body"]');
        const wrap = view.querySelector('[data-role="table-wrap"]');
        const empty = view.querySelector('[data-role="empty"]');
        tbody.innerHTML = '';

        if (!state.cache.length) {
            state.selectedId = null;
            updateListToolbar(entity);
            wrap.classList.add('hidden');
            empty.classList.remove('hidden');
            setListStatus(entity, 'Записей не найдено.', false);
            return;
        }

        if (!state.cache.some((i) => String(i.id) === String(state.selectedId))) state.selectedId = null;
        empty.classList.add('hidden');
        wrap.classList.remove('hidden');

        state.cache.forEach((item) => {
            const tr = document.createElement('tr');
            const deleted = isDeleted(entity, item);
            tr.className = (deleted ? 'row--deleted ' : '') + (String(item.id) === String(state.selectedId) ? 'row--selected' : '');
            tr.dataset.id = item.id;
            const href = '#/' + entityMeta[entity].hash + '/' + item.id;
            let cells = '<td class="col-mark">' + renderMarkCell(entity, item) + '</td>';

            if (entity === 'units') {
                cells += '<td class="col-id"><a class="entity-table__link" href="' + href + '">' + escapeHtml(item.id) + '</a></td>';
                cells += '<td><a class="entity-table__link" href="' + href + '">' + escapeHtml(item.name) + '</a></td>';
                cells += '<td class="col-code">' + escapeHtml(item.code) + '</td>';
                cells += '<td>' + escapeHtml(item.description || '—') + '</td>';
            } else if (entity === 'users') {
                cells += '<td class="col-id"><a class="entity-table__link" href="' + href + '">' + escapeHtml(item.id) + '</a></td>';
                cells += '<td><a class="entity-table__link" href="' + href + '">' + escapeHtml(item.username) + '</a></td>';
                cells += '<td>' + escapeHtml(item.lastName || '—') + '</td>';
                cells += '<td>' + escapeHtml(item.firstName || '—') + '</td>';
                cells += '<td>' + escapeHtml(item.middleName || '—') + '</td>';
                cells += '<td class="col-bool">' + (item.enabled ? 'Да' : 'Нет') + '</td>';
            } else if (entity === 'roles') {
                cells += '<td class="col-id"><a class="entity-table__link" href="' + href + '">' + escapeHtml(item.id) + '</a></td>';
                cells += '<td><a class="entity-table__link" href="' + href + '">' + escapeHtml(item.name) + '</a></td>';
            } else if (entity === 'permissions') {
                cells += '<td><a class="entity-table__link" href="' + href + '">' + escapeHtml(item.name) + '</a></td>';
                cells += '<td class="col-code">' + escapeHtml(item.code) + '</td>';
            }

            tr.innerHTML = cells;
            tr.addEventListener('click', (e) => {
                if (e.target.closest('a')) return;
                state.selectedId = item.id;
                renderEntityTable(entity, state.cache);
            });
            tr.addEventListener('dblclick', () => openEntityCard(entity, item.id));
            tbody.appendChild(tr);
        });

        updateListToolbar(entity);
        setListStatus(entity, 'Записей: ' + state.cache.length, false);
    }

    async function loadEntityList(entity) {
        setListStatus(entity, 'Загрузка…', false);
        try {
            const data = await apiRequest(apiUrl(entity));
            let items = data[entityMeta[entity].listKey] || [];
            if (entity === 'permissions') {
                if (permissionIdByCode.size === 0) await buildPermissionIdMap();
                items = items.map((p) => ({ ...p, id: permissionIdByCode.get(p.code) || p.id }));
                permissionsCatalog = items.filter((p) => p.id);
            }
            if (entity === 'roles') rolesCatalog = items;
            renderEntityTable(entity, items);
        } catch (err) {
            const view = getListView(entity);
            view.querySelector('[data-role="table-wrap"]')?.classList.add('hidden');
            view.querySelector('[data-role="empty"]')?.classList.remove('hidden');
            setListStatus(entity, 'Ошибка: ' + err.message, true);
        }
    }

    function getInput(cardRoot, name) {
        return cardRoot.querySelector('[data-input="' + name + '"]');
    }

    function getCardValue(cardRoot, name) {
        const el = getInput(cardRoot, name);
        if (!el) return '';
        if (el.type === 'checkbox') return el.checked;
        return el.value;
    }

    function setCardValue(cardRoot, name, value) {
        const el = getInput(cardRoot, name);
        if (!el) return;
        if (el.type === 'checkbox') el.checked = !!value;
        else el.value = value == null ? '' : value;
    }

    function clearFieldErrors() {
        validationQueue = [];
        validationIndex = 0;
        document.querySelectorAll('.field-input--error').forEach((el) => el.classList.remove('field-input--error'));
        els.validationBubble?.classList.add('hidden');
    }

    function showFieldErrors(fieldErrors, cardBody) {
        validationQueue = fieldErrors.slice();
        showValidationErrorAt(0, cardBody);
    }

    function showValidationErrorAt(index, cardBody) {
        if (!validationQueue.length) return;
        validationIndex = Math.max(0, Math.min(index, validationQueue.length - 1));
        const item = validationQueue[validationIndex];
        const input = cardBody?.querySelector('[data-input="' + item.field + '"]') || cardBody?.querySelector('[data-field="' + item.field + '"] input');
        document.querySelectorAll('.field-input--error').forEach((el) => el.classList.remove('field-input--error'));
        if (input) {
            input.classList.add('field-input--error');
            input.focus();
        }
        els.validationMessage.textContent = item.message;
        const hasMultiple = validationQueue.length > 1;
        els.validationNav.classList.toggle('hidden', !hasMultiple);
        if (hasMultiple) {
            els.validationCounter.textContent = (validationIndex + 1) + ' / ' + validationQueue.length;
            els.validationPrev.disabled = validationIndex === 0;
            els.validationNext.disabled = validationIndex === validationQueue.length - 1;
        }
        els.validationBubble.classList.remove('hidden');
    }

    function setToolbarButtonsVisible(root, selector, visible) {
        root.querySelectorAll(selector).forEach((btn) => {
            btn.classList.toggle('hidden', !visible);
            if (visible) btn.style.removeProperty('display');
        });
    }

    function setPasswordChangeInputsEnabled(root, enabled) {
        root.querySelectorAll('[data-role="change-password-panel"] input').forEach((input) => {
            input.disabled = !enabled;
        });
        const applyBtn = root.querySelector('[data-action="apply-password-change"]');
        if (applyBtn) applyBtn.disabled = !enabled;
    }

    function applyReadonlyMode(entity, readonly) {
        readonly = !!readonly;
        const root = getCardRoot(entity);
        const state = cardState[entity];
        state.readonly = readonly;
        root.classList.toggle('entity-card--readonly', readonly);
        const banner = root.querySelector('[data-role="readonly-banner"]');
        if (banner) {
            banner.classList.toggle('hidden', !readonly);
            if (readonly && isEntityReadOnlyByPermission(entity) && !state.systemDefined) {
                banner.textContent = 'Только просмотр — недостаточно прав на изменение';
            }
        }
        setToolbarButtonsVisible(root, '[data-action="save"], [data-action="save-close"], [data-action="more"]', !readonly);
        setToolbarButtonsVisible(root, '[data-action="apply-password-change"]', !readonly);
        root.querySelectorAll('[data-action="mark-deleted"], [data-action="unmark-deleted"], [data-action="hard-delete"]').forEach((btn) => {
            btn.classList.toggle('hidden', readonly);
            if (!readonly) btn.style.removeProperty('display');
        });
        root.querySelectorAll('input:not([type="hidden"]), textarea, select').forEach((input) => {
            if (input.closest('[data-role="change-password-panel"]')) return;
            if (!input.hasAttribute('readonly')) input.disabled = readonly;
        });
        updateCardActions(entity);
    }

    function updateUserPasswordUi(root, state) {
        const id = root.querySelector('[data-field="id"]')?.value;
        const hasId = !!id;
        const canChange = hasId && !state.readonly && !state.systemDefined;
        root.querySelector('[data-role="create-password-block"]')?.classList.toggle('hidden', hasId);
        root.querySelector('[data-role="enabled-field"]')?.classList.toggle('hidden', !hasId);
        root.querySelector('[data-role="password-change-trigger"]')?.classList.toggle('hidden', !canChange);
        root.querySelector('[data-role="more-menu"] [data-action="change-password"]')?.classList.toggle('hidden', !canChange);
        setPasswordChangeInputsEnabled(root, canChange);
        if (!canChange) {
            root.querySelector('[data-role="change-password-panel"]')?.classList.add('hidden');
        }
    }

    function updateCardActions(entity) {
        const root = getCardRoot(entity);
        const state = cardState[entity];
        const id = root.querySelector('[data-field="id"]')?.value;
        const hasId = !!id;
        if (entity === 'users') {
            updateUserPasswordUi(root, state);
        }
        root.querySelectorAll('[data-action="mark-deleted"]').forEach((markBtn) => {
            markBtn.disabled = !hasId || state.deleted || state.readonly;
            markBtn.style.display = state.readonly ? 'none' : '';
        });
        root.querySelectorAll('[data-action="unmark-deleted"]').forEach((unmarkBtn) => {
            unmarkBtn.disabled = !hasId || !state.deleted || state.readonly || state.systemDefined;
            unmarkBtn.style.display = state.readonly || state.systemDefined ? 'none' : '';
        });
        root.querySelectorAll('[data-action="hard-delete"]').forEach((hardBtn) => {
            hardBtn.disabled = !hasId || state.readonly;
            hardBtn.style.display = state.readonly ? 'none' : '';
        });
        root.querySelectorAll('[data-action="copy-link"]').forEach((btn) => { btn.disabled = !hasId; });
    }

    async function populateRolesChecklist(container, selectedIds) {
        if (!rolesCatalog.length) {
            try {
                const data = await apiRequest(apiUrl('roles'));
                rolesCatalog = data.roles || [];
            } catch (e) { /* */ }
        }
        container.innerHTML = '';
        const selected = new Set((selectedIds || []).map(String));
        rolesCatalog.forEach((role) => {
            const label = document.createElement('label');
            label.className = 'checkbox-list__item';
            label.innerHTML = '<input type="checkbox" value="' + role.id + '"' + (selected.has(String(role.id)) ? ' checked' : '') + '> ' + escapeHtml(role.name);
            container.appendChild(label);
        });
    }

    async function populatePermissionsChecklist(container, selectedCodesOrIds, byCode) {
        if (!permissionsCatalog.length) {
            try {
                if (permissionIdByCode.size === 0) await buildPermissionIdMap();
                const data = await apiRequest(apiUrl('permissions'));
                permissionsCatalog = (data.permissions || []).map((p) => ({ ...p, id: permissionIdByCode.get(p.code) }));
            } catch (e) { /* */ }
        }
        container.innerHTML = '';
        const selected = new Set((selectedCodesOrIds || []).map(String));
        permissionsCatalog.forEach((perm) => {
            const label = document.createElement('label');
            label.className = 'checkbox-list__item';
            const val = byCode ? perm.code : perm.id;
            const checked = byCode ? selected.has(perm.code) : selected.has(String(perm.id));
            label.innerHTML = '<input type="checkbox" value="' + escapeHtml(String(val)) + '"' + (checked ? ' checked' : '') + '> ' + escapeHtml(perm.name);
            container.appendChild(label);
        });
    }

    function getCheckedValues(container) {
        return Array.from(container.querySelectorAll('input[type="checkbox"]:checked')).map((cb) => cb.value);
    }

    function clearEntityCard(entity, keepId) {
        const root = getCardRoot(entity);
        const state = cardState[entity];
        state.deleted = false;
        state.systemDefined = false;
        if (entity === 'users') state.usernameManual = false;
        if (!keepId) root.querySelector('[data-field="id"]').value = '';
        root.querySelectorAll('[data-input]').forEach((input) => {
            if (input.type === 'checkbox') input.checked = entity === 'users' && input.dataset.input === 'enabled';
            else input.value = '';
        });
        root.querySelector('[data-role="change-password-panel"]')?.classList.add('hidden');
        root.querySelector('[data-role="create-password-block"]')?.classList.toggle('hidden', !!keepId);
        root.querySelector('[data-role="enabled-field"]')?.classList.toggle('hidden', !keepId);
        root.querySelector('[data-role="password-change-trigger"]')?.classList.add('hidden');
        clearFieldErrors();
        applyReadonlyMode(entity, false);
    }

    async function loadEntityIntoCard(entity, id) {
        const root = getCardRoot(entity);
        const state = cardState[entity];
        try {
            const item = await apiRequest(apiUrl(entity, id));
            root.querySelector('[data-field="id"]').value = item.id;

            if (entity === 'units') {
                setCardValue(root, 'name', item.name);
                setCardValue(root, 'code', item.code);
                setCardValue(root, 'description', item.description);
                state.deleted = isDeleted(entity, item);
                state.systemDefined = isSystemDefined(entity, item);
            } else if (entity === 'users') {
                setCardValue(root, 'username', item.username);
                setCardValue(root, 'lastName', item.lastName);
                setCardValue(root, 'firstName', item.firstName);
                setCardValue(root, 'middleName', item.middleName);
                setCardValue(root, 'enabled', item.enabled !== false);
                state.deleted = isDeleted(entity, item);
                state.systemDefined = isSystemDefined(entity, item);
                await populateRolesChecklist(root.querySelector('[data-role="roles-checklist"]'), (item.roles || []).map((r) => r.id));
            } else if (entity === 'roles') {
                setCardValue(root, 'name', item.name);
                const permIds = (item.permissions || []).map((p) => permissionIdByCode.get(p.code) || p.id).filter(Boolean);
                await populatePermissionsChecklist(root.querySelector('[data-role="permissions-checklist"]'), permIds, false);
                state.deleted = isDeleted(entity, item);
                state.systemDefined = isSystemDefined(entity, item);
            } else if (entity === 'permissions') {
                setCardValue(root, 'name', item.name);
                setCardValue(root, 'code', item.code);
                state.systemDefined = true;
                state.readonly = true;
            }

            const label = entityMeta[entity].label(item);
            const header = root.querySelector('[data-role="header"]');
            header.textContent = label + (state.deleted ? ' (помечена на удаление)' : '');
            applyReadonlyMode(entity, state.systemDefined || entityMeta[entity].readOnly || isEntityReadOnlyByPermission(entity));
            updateCardActions(entity);

            const tab = openTabs.find((t) => t.id === entity.replace(/s$/, '') + '-' + id);
            if (tab) tab.label = label;
            renderTabs();
            return true;
        } catch (err) {
            showToast(err.message || 'Не удалось загрузить', true);
            return false;
        }
    }

    async function openEntityCard(entity, recordId, options = {}) {
        const meta = entityMeta[entity];
        if (!canReadEntity(entity)) {
            denyAccess();
            return false;
        }
        if (recordId == null && !canWriteEntity(entity)) {
            denyAccess('Недостаточно прав для создания записи');
            return false;
        }
        if (recordId != null) {
            clearEntityCard(entity, false);
            getCardRoot(entity).querySelector('[data-role="header"]').textContent = 'Загрузка…';
            const loaded = await loadEntityIntoCard(entity, recordId);
            if (!loaded) {
                navigateTo(meta.listSection, { syncHash: false });
                return false;
            }
        } else {
            const root = getCardRoot(entity);
            clearEntityCard(entity, false);
            cardState[entity].deleted = false;
            root.querySelector('[data-role="header"]').textContent = 'Новая запись';
            if (entity === 'users') {
                root.querySelector('[data-role="create-password-block"]')?.classList.remove('hidden');
                root.querySelector('[data-role="enabled-field"]')?.classList.add('hidden');
                await populateRolesChecklist(root.querySelector('[data-role="roles-checklist"]'), []);
            }
            if (entity === 'roles') {
                await populatePermissionsChecklist(root.querySelector('[data-role="permissions-checklist"]'), [], false);
            }
            updateCardActions(entity);
        }

        const tabId = recordId == null ? entity + '-new' : entity.replace(/s$/, '') + '-' + recordId;
        const label = recordId == null ? 'Новая' : getCardRoot(entity).querySelector('[data-role="header"]').textContent;
        openTab({
            id: tabId,
            sectionId: meta.cardSection,
            entity,
            recordId,
            label: recordId == null ? 'Новая запись' : label,
            icon: sections[meta.listSection]?.tabIcon || '📄',
            fromSection: meta.listSection,
        });
        activeTabId = tabId;
        activeCardEntity = entity;
        showViewForTab(getActiveTab());
        updateSidebarActive();
        renderTabs();
        if (options.syncHash !== false) syncHash();
        return true;
    }

    function buildUnitPayload(root) {
        return {
            name: getCardValue(root, 'name').trim(),
            code: getCardValue(root, 'code').trim(),
            description: getCardValue(root, 'description').trim(),
        };
    }

    function buildUserPayload(root, isCreate) {
        const roles = getCheckedValues(root.querySelector('[data-role="roles-checklist"]')).map(Number);
        const payload = {
            username: getCardValue(root, 'username').trim(),
            lastName: getCardValue(root, 'lastName').trim() || null,
            firstName: getCardValue(root, 'firstName').trim() || null,
            middleName: getCardValue(root, 'middleName').trim() || null,
            rolesIdentifiers: roles,
        };
        if (isCreate) payload.password = getCardValue(root, 'password');
        else {
            payload.enabled = getCardValue(root, 'enabled');
        }
        return payload;
    }

    function buildRolePayload(root) {
        return {
            name: getCardValue(root, 'name').trim(),
            permissionsIdentifiers: getCheckedValues(root.querySelector('[data-role="permissions-checklist"]'))
                .map(Number)
                .filter((n) => !Number.isNaN(n) && n > 0),
        };
    }

    function validateUserPassword(root, isCreate) {
        if (!isCreate) return true;
        const password = getCardValue(root, 'password');
        const confirm = getCardValue(root, 'passwordConfirm');
        const { rules } = evaluatePasswordStrength(password);
        if (!rules.length || !rules.lower || !rules.upper || !rules.digit) {
            showToast('Пароль не соответствует требованиям безопасности', true);
            return false;
        }
        if (password !== confirm) {
            showToast('Пароли не совпадают', true);
            return false;
        }
        return true;
    }

    async function saveEntity(entity, closeAfter) {
        const root = getCardRoot(entity);
        const body = root.querySelector('[data-role="body"]');
        if (cardState[entity].readonly || !canWriteEntity(entity)) return false;
        clearFieldErrors();
        const id = root.querySelector('[data-field="id"]').value;
        const isCreate = !id;

        if (entity === 'users' && !validateUserPassword(root, isCreate)) return false;

        let payload;
        if (entity === 'units') payload = buildUnitPayload(root);
        else if (entity === 'users') payload = buildUserPayload(root, isCreate);
        else if (entity === 'roles') payload = buildRolePayload(root);
        else return false;

        const saveButtons = root.querySelectorAll('[data-action="save"], [data-action="save-close"]');
        saveButtons.forEach((b) => b.disabled = true);

        try {
            const saved = isCreate
                ? await apiRequest(apiUrl(entity), { method: 'POST', body: JSON.stringify(payload) })
                : await apiRequest(apiUrl(entity, id), { method: 'PATCH', body: JSON.stringify(payload) });

            root.querySelector('[data-field="id"]').value = saved.id;
            getCardRoot(entity).querySelector('[data-role="header"]').textContent = entityMeta[entity].label(saved);

            const tab = getActiveTab();
            if (tab && tab.sectionId === entityMeta[entity].cardSection) {
                if (tab.id.endsWith('-new')) {
                    tab.id = entity.replace(/s$/, '') + '-' + saved.id;
                    tab.recordId = saved.id;
                    activeTabId = tab.id;
                }
                tab.label = entityMeta[entity].label(saved);
                tab.recordId = saved.id;
            }

            if (entity === 'users' && isCreate) {
                cardState.users.usernameManual = false;
            }
            const state = cardState[entity];
            state.systemDefined = isSystemDefined(entity, saved);
            state.deleted = isDeleted(entity, saved);
            applyReadonlyMode(entity, state.systemDefined || entityMeta[entity].readOnly || isEntityReadOnlyByPermission(entity));

            showToast('Запись сохранена.', false);
            updateCardActions(entity);
            renderTabs();
            syncHash(isCreate);
            if (openTabs.some((t) => t.sectionId === entityMeta[entity].listSection)) await loadEntityList(entity);

            if (closeAfter) {
                const closingId = activeTabId;
                closeTab(closingId);
                if (!openTabs.some((t) => t.sectionId === entityMeta[entity].listSection)) navigateTo(entityMeta[entity].listSection);
            }
            return true;
        } catch (err) {
            if (err.fieldErrors && err.fieldErrors.length) showFieldErrors(err.fieldErrors, body);
            else showToast(err.message || 'Ошибка сохранения', true);
            return false;
        } finally {
            saveButtons.forEach((b) => b.disabled = cardState[entity].readonly);
            updateCardActions(entity);
        }
    }

    async function applyPasswordChange(entity) {
        const root = getCardRoot(entity);
        const id = root.querySelector('[data-field="id"]').value;
        if (!id) return;
        const oldPassword = getCardValue(root, 'oldPassword');
        const newPassword = getCardValue(root, 'newPassword');
        const confirm = getCardValue(root, 'newPasswordConfirm');
        const { rules } = evaluatePasswordStrength(newPassword);
        if (!rules.length || !rules.lower || !rules.upper || !rules.digit) {
            showToast('Новый пароль не соответствует требованиям', true);
            return;
        }
        if (newPassword !== confirm) {
            showToast('Подтверждение пароля не совпадает', true);
            return;
        }
        try {
            await apiRequest(apiUrl(entity, id + '/change-password'), {
                method: 'PATCH',
                body: JSON.stringify({ oldPassword, newPassword }),
            });
            showToast('Пароль изменён.', false);
            root.querySelector('[data-role="change-password-panel"]')?.classList.add('hidden');
            setCardValue(root, 'oldPassword', '');
            setCardValue(root, 'newPassword', '');
            setCardValue(root, 'newPasswordConfirm', '');
        } catch (err) {
            showToast(err.message || 'Ошибка смены пароля', true);
        }
    }

    async function markEntityDeleted(entity, deleted) {
        const root = getCardRoot(entity);
        const id = root.querySelector('[data-field="id"]').value;
        if (!id || cardState[entity].readonly) return;
        const action = deleted ? 'mark-deleted' : 'unmark-deleted';
        const msg = deleted ? 'Пометить на удаление?' : 'Снять пометку удаления?';
        if (!confirm(msg)) return;
        try {
            await apiRequest(apiUrl(entity, id + '/' + action), { method: 'PATCH' });
            cardState[entity].deleted = deleted;
            markSessionDeleted(entity, id, deleted);
            const header = root.querySelector('[data-role="header"]');
            const base = header.textContent.replace(' (помечена на удаление)', '');
            header.textContent = base + (deleted ? ' (помечена на удаление)' : '');
            updateCardActions(entity);
            showToast(deleted ? 'Помечено на удаление.' : 'Пометка снята.', false);
            if (openTabs.some((t) => t.sectionId === entityMeta[entity].listSection)) await loadEntityList(entity);
        } catch (err) {
            showToast(err.message, true);
        }
    }

    async function hardDeleteEntity(entity) {
        const root = getCardRoot(entity);
        const id = root.querySelector('[data-field="id"]').value;
        if (!id || cardState[entity].readonly) return;
        if (!confirm('Удалить запись безвозвратно?')) return;
        try {
            await apiRequest(apiUrl(entity, id), { method: 'DELETE' });
            showToast('Запись удалена.', false);
            const tabId = activeTabId;
            closeTab(tabId);
            await loadEntityList(entity);
            if (!openTabs.some((t) => t.sectionId === entityMeta[entity].listSection)) navigateTo(entityMeta[entity].listSection);
        } catch (err) {
            showToast(err.message, true);
        }
    }

    function getShareableUrl() {
        return window.location.origin + window.location.pathname + hashFromTab(getActiveTab());
    }

    async function copyCurrentLink() {
        const tab = getActiveTab();
        if (!tab || !tab.recordId) {
            showToast('Ссылка доступна после сохранения.', true);
            return;
        }
        try {
            await navigator.clipboard.writeText(getShareableUrl());
            showToast('Ссылка скопирована', false);
        } catch (e) {
            showToast('Не удалось скопировать', true);
        }
    }

    function bindListEvents(entity) {
        const view = getListView(entity);
        if (!view || view.dataset.bound) return;
        view.dataset.bound = '1';
        view.querySelector('[data-action="refresh"]')?.addEventListener('click', () => loadEntityList(entity));
        view.querySelector('[data-action="create"]')?.addEventListener('click', () => openEntityCard(entity, null));
        view.querySelector('[data-action="edit"]')?.addEventListener('click', () => {
            const id = listState[entity].selectedId;
            if (id == null) return showToast('Выберите запись', true);
            openEntityCard(entity, id);
        });
        view.querySelector('[data-action="view"]')?.addEventListener('click', () => {
            const id = listState[entity].selectedId;
            if (id == null) return showToast('Выберите запись', true);
            openEntityCard(entity, id);
        });
        view.querySelector('[data-action="copy-link"]')?.addEventListener('click', async () => {
            const id = listState[entity].selectedId;
            if (id == null) return showToast('Выберите запись', true);
            const url = window.location.origin + window.location.pathname + '#/' + entityMeta[entity].hash + '/' + id;
            try {
                await navigator.clipboard.writeText(url);
                showToast('Ссылка скопирована', false);
            } catch (e) { showToast('Ошибка копирования', true); }
        });
    }

    function bindCardEvents(entity) {
        const root = getCardRoot(entity);
        if (!root || root.dataset.bound) return;
        root.dataset.bound = '1';
        const body = root.querySelector('[data-role="body"]');

        root.querySelectorAll('[data-action="save"]').forEach((btn) => btn.addEventListener('click', () => saveEntity(entity, false)));
        root.querySelectorAll('[data-action="save-close"]').forEach((btn) => btn.addEventListener('click', () => saveEntity(entity, true)));
        root.querySelector('[data-action="close-card"]')?.addEventListener('click', () => {
            const tab = getActiveTab();
            if (tab && !tab.pinned) closeTab(activeTabId);
        });
        root.querySelectorAll('[data-action="copy-link"]').forEach((btn) => btn.addEventListener('click', copyCurrentLink));
        root.querySelectorAll('[data-action="mark-deleted"]').forEach((btn) => btn.addEventListener('click', () => markEntityDeleted(entity, true)));
        root.querySelectorAll('[data-action="unmark-deleted"]').forEach((btn) => btn.addEventListener('click', () => markEntityDeleted(entity, false)));
        root.querySelectorAll('[data-action="hard-delete"]').forEach((btn) => btn.addEventListener('click', () => hardDeleteEntity(entity)));
        root.querySelectorAll('[data-action="change-password"]').forEach((btn) => btn.addEventListener('click', () => {
            const panel = root.querySelector('[data-role="change-password-panel"]');
            if (!panel) return;
            panel.classList.toggle('hidden');
            if (!panel.classList.contains('hidden') && entity === 'users') {
                const state = cardState.users;
                const hasId = !!root.querySelector('[data-field="id"]')?.value;
                const canChange = hasId && !state.readonly && !state.systemDefined;
                setPasswordChangeInputsEnabled(root, canChange);
            }
        }));
        root.querySelector('[data-action="apply-password-change"]')?.addEventListener('click', () => applyPasswordChange(entity));

        const moreBtn = root.querySelector('[data-action="more"]');
        const moreMenu = root.querySelector('[data-role="more-menu"]');
        moreBtn?.addEventListener('click', (e) => {
            e.stopPropagation();
            moreMenu?.classList.toggle('dropdown__menu--open');
        });

        if (entity === 'users') {
            const usernameInput = getInput(root, 'username');
            ['lastName', 'firstName', 'middleName'].forEach((field) => {
                getInput(root, field)?.addEventListener('input', () => {
                    if (!cardState.users.usernameManual && !root.querySelector('[data-field="id"]').value) {
                        setCardValue(root, 'username', generateUsername(
                            getCardValue(root, 'lastName'),
                            getCardValue(root, 'firstName'),
                            getCardValue(root, 'middleName')
                        ));
                    }
                });
            });
            usernameInput?.addEventListener('input', () => { cardState.users.usernameManual = true; });
            getInput(root, 'password')?.addEventListener('input', (e) => {
                updatePasswordStrengthUI(root.querySelector('[data-role="strength"]'), e.target.value);
            });
            getInput(root, 'newPassword')?.addEventListener('input', (e) => {
                updatePasswordStrengthUI(root.querySelector('[data-role="change-strength"]'), e.target.value);
            });
        }

        root.querySelectorAll('[data-toggle-password]').forEach((btn) => {
            btn.addEventListener('click', () => {
                const input = btn.parentElement.querySelector('input');
                if (!input) return;
                input.type = input.type === 'password' ? 'text' : 'password';
            });
        });

        body?.querySelectorAll('input, textarea').forEach((input) => {
            input.addEventListener('input', clearFieldErrors);
        });
    }

    function runSearch(query) {
        const q = query.trim().toLowerCase();
        if (!q) {
            els.searchDropdown.classList.remove('search-dropdown--open');
            els.searchDropdown.innerHTML = '';
            return;
        }
        const results = [];
        let hashPart = query;
        const hashIdx = query.indexOf('#');
        if (hashIdx !== -1) {
            hashPart = query.substring(hashIdx);
        } else {
            hashPart = '#' + query.replace(/^\//, '');
        }
        const navRoute = parseHash(hashPart);
        if (navRoute && navRoute.type !== 'warehouse' && navRoute.type !== 'home' && canAccessRoute(navRoute)) {
            results.push({ title: 'Перейти: ' + routeTitle(navRoute), path: routePath(navRoute), navRoute });
        }
        searchIndex.forEach((item) => {
            if (!canAccessSection(item.sectionId)) return;
            if (item.title.toLowerCase().includes(q) || item.keywords.some((k) => k.includes(q) || q.includes(k))) results.push(item);
        });
        els.searchDropdown.innerHTML = results.length
            ? results.map((item, idx) => '<button type="button" class="search-result" data-idx="' + idx + '"><div class="search-result__title">' + escapeHtml(item.title) + '</div><div class="search-result__path">' + escapeHtml(item.path || (item.navRoute ? routePath(item.navRoute) : '')) + '</div></button>').join('')
            : '<div class="search-dropdown__empty">Ничего не найдено</div>';
        els.searchDropdown.querySelectorAll('.search-result').forEach((btn, idx) => {
            btn.addEventListener('click', () => {
                const item = results[idx];
                els.globalSearch.value = '';
                els.searchDropdown.classList.remove('search-dropdown--open');
                if (item.navRoute) navigateByRoute(item.navRoute);
                else if (item.entity) navigateTo(entityMeta[item.entity].listSection);
                else navigateTo(item.sectionId);
            });
        });
        els.searchDropdown.classList.add('search-dropdown--open');
    }

    function initSidebar() {
        els.sidebarNav.innerHTML = '';
        [{ section: 'warehouse', ...sections.warehouse }, { section: 'nsiAdmin', ...sections.nsiAdmin }].forEach((item) => {
            const li = document.createElement('li');
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'sidebar__link';
            btn.dataset.section = item.section;
            btn.innerHTML = '<span class="sidebar__icon">' + item.sidebarIcon + '</span><span class="sidebar__label">' + item.sidebarLabel + '</span>';
            btn.addEventListener('click', () => navigateTo(item.section));
            li.appendChild(btn);
            els.sidebarNav.appendChild(li);
        });
        applyPermissionUi();
    }

    function bindGlobalEvents() {
        document.getElementById('btn-load-units')?.addEventListener('click', () => navigateTo('units'));
        document.getElementById('btn-load-admin-settings')?.addEventListener('click', () => navigateTo('adminSettings'));
        document.getElementById('btn-load-users')?.addEventListener('click', () => navigateTo('users'));
        document.getElementById('btn-load-roles')?.addEventListener('click', () => navigateTo('roles'));
        document.getElementById('btn-load-permissions')?.addEventListener('click', () => navigateTo('permissions'));

        els.globalSearch?.addEventListener('input', () => runSearch(els.globalSearch.value));
        els.globalSearch?.addEventListener('focus', () => { if (els.globalSearch.value.trim()) runSearch(els.globalSearch.value); });
        els.validationClose?.addEventListener('click', clearFieldErrors);
        els.validationPrev?.addEventListener('click', () => showValidationErrorAt(validationIndex - 1, getCardRoot(activeCardEntity)?.querySelector('[data-role="body"]')));
        els.validationNext?.addEventListener('click', () => showValidationErrorAt(validationIndex + 1, getCardRoot(activeCardEntity)?.querySelector('[data-role="body"]')));

        document.getElementById('btn-logout')?.addEventListener('click', () => {
            logout();
        });

        document.addEventListener('click', (e) => {
            if (!e.target.closest('[data-role="more-dropdown"]')) {
                document.querySelectorAll('.dropdown__menu--open').forEach((m) => m.classList.remove('dropdown__menu--open'));
            }
            if (!e.target.closest('.app-header__search-wrap')) els.searchDropdown?.classList.remove('search-dropdown--open');
        });
    }

    async function logout() {
        const refreshToken = getRefreshToken();
        if (refreshToken) {
            try {
                await fetch(API.logout, {
                    method: 'POST',
                    headers: authJsonHeaders(),
                    body: JSON.stringify({ refreshToken }),
                });
            } catch (e) { /* локальный выход всё равно выполняем */ }
        }
        handleUnauthorized();
    }

    function handleUnauthorized() {
        clearTokens();
        userPermissionCodes = new Set();
        currentUsername = '';
        const usernameEl = document.getElementById('header-username');
        if (usernameEl) usernameEl.textContent = '';
        appInitialized = false;
        document.getElementById('app-root').style.display = 'none';
        document.getElementById('login-overlay').style.display = 'flex';
    }

    function initAuth() {
        const loginForm = document.getElementById('login-form');
        const loginError = document.getElementById('login-error');

        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('login-username').value;
            const password = document.getElementById('login-password').value;

            try {
                const res = await fetch(API.login, {
                    method: 'POST',
                    headers: authJsonHeaders(),
                    body: JSON.stringify({ username, password }),
                });

                if (res.status === 401) {
                    loginError.textContent = 'Неверный логин или пароль';
                    loginError.classList.remove('hidden');
                    return;
                }

                if (!res.ok) {
                    loginError.textContent = 'Не удалось войти в систему';
                    loginError.classList.remove('hidden');
                    return;
                }

                const data = await res.json();
                if (!data.accessToken || !data.refreshToken) {
                    loginError.textContent = 'Не удалось войти в систему';
                    loginError.classList.remove('hidden');
                    return;
                }

                storeTokens(data);
                document.getElementById('login-overlay').style.display = 'none';
                document.getElementById('app-root').style.display = 'flex';
                loginError.classList.add('hidden');

                await initApp();
            } catch (err) {
                loginError.textContent = 'Ошибка соединения с сервером';
                loginError.classList.remove('hidden');
            }
        });

        if (getAccessToken() || getRefreshToken()) {
            document.getElementById('login-overlay').style.display = 'none';
            document.getElementById('app-root').style.display = 'flex';
            initApp();
        } else {
            document.getElementById('login-overlay').style.display = 'flex';
            document.getElementById('app-root').style.display = 'none';
        }
    }

    let appInitialized = false;

    async function initApp() {
        try {
            await loadCurrentUser();
        } catch (err) {
            if (err.status === 401) handleUnauthorized();
            else showToast(err.message || 'Не удалось загрузить данные пользователя', true);
            return;
        }

        applyPermissionUi();

        if (appInitialized) {
            if (permissionIdByCode.size === 0) await buildPermissionIdMap();
            const route = parseHash(location.hash);
            if (route && !canAccessRoute(route)) {
                switchTab('home');
                syncHash(true);
            } else {
                await initRouter();
            }
            return;
        }

        appInitialized = true;
        initSidebar();
        ['units', 'users', 'roles', 'permissions'].forEach((e) => { bindListEvents(e); bindCardEvents(e); });
        bindGlobalEvents();
        await buildPermissionIdMap();
        const route = parseHash(location.hash);
        if (route && !canAccessRoute(route)) {
            skipHashSync = true;
            switchTab('home');
            syncHash(true);
            skipHashSync = false;
        } else {
            await initRouter();
        }
    }

    initAuth();
})();
