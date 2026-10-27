// HyperMark — native window shell (Electron main process).
// Creates a real desktop application window with no browser chrome:
// dark title, taskbar presence, native file dialogs.
//
// Usage: electron main.js <app-url>

const { app, BrowserWindow, Menu, shell } = require("electron");
const path = require("path");
const os = require("os");
const fs = require("fs");

// Confine the Chromium profile to a persistent, user-owned location
// (NOT /tmp — that gets wiped on reboot and would erase localStorage)
const userData = process.env.HYPERMARK_USER_DATA ||
    path.join(os.homedir(), ".local", "state", "hypermark", "electron-profile");
try { fs.mkdirSync(userData, { recursive: true }); } catch (_e) {}

// Offline app: no background fetching, component updates, or proxy use
app.commandLine.appendSwitch("disable-component-update");
app.commandLine.appendSwitch("disable-background-networking");
app.commandLine.appendSwitch("no-proxy-server");

const url = process.argv[2] || "http://127.0.0.1/";
let win = null;

function createWindow() {
    win = new BrowserWindow({
        width: 1280,
        height: 830,
        minWidth: 620,
        minHeight: 420,
        backgroundColor: "#070B13",
        title: "HyperMark",
        show: false,
        webPreferences: {
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: true
        }
    });

    // Clean application shell: no menu bar
    Menu.setApplicationMenu(null);

    // External links open in the system browser; blob: URLs (e.g. the diagram
    // pop-out) get their own native child window; everything else is denied.
    win.webContents.setWindowOpenHandler(({ url: target }) => {
        if (target.startsWith("blob:")) {
            return {
                action: "allow",
                overrideBrowserWindowOptions: {
                    width: 1250, height: 830,
                    backgroundColor: "#070B13",
                    title: "HyperMark — Diagram",
                    webPreferences: { contextIsolation: true, nodeIntegration: false, sandbox: true }
                }
            };
        }
        shell.openExternal(target);
        return { action: "deny" };
    });

    win.loadURL(url);
    win.once("ready-to-show", () => win.show());
    win.on("closed", () => { win = null; });
    return win;
}

app.whenReady().then(() => {
    app.setPath("userData", userData);
    app.setAppUserModelId("app.hypermark.desktop");
    createWindow();

    app.on("activate", () => {
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

app.on("window-all-closed", () => {
    app.quit();
});
