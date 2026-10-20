Name:           hypermark
Version:        1.0.0
Release:        1.fc44
Summary:        Cyberpunk Markdown Editor & Graph Topology Studio for Fedora
License:        Apache-2.0
URL:            https://github.com/hypermark/hypermark
BuildArch:      x86_64

%description
HyperMark is a modern, dark-translucent Markdown editor with real-time LaTeX
mathematics via KaTeX, CTAN macro support, Cytoscape and Mermaid diagram
rendering, and an interactive 2D network topology graph with smooth curved
Bézier connectors and pulsing radar animation.

%prep
# No prep required

%build
# No build required for pre-packaged assets

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/usr/bin
mkdir -p %{buildroot}/usr/share/hypermark
mkdir -p %{buildroot}/usr/share/applications
mkdir -p %{buildroot}/usr/share/icons/hicolor/scalable/apps
mkdir -p %{buildroot}/usr/share/icons/hicolor/256x256/apps

# Copy launcher binary
install -m 755 %{_sourcedir}/hypermark-launcher.py %{buildroot}/usr/bin/hypermark

# Copy application web assets
cp -r %{_sourcedir}/app/* %{buildroot}/usr/share/hypermark/

# Copy desktop entry and icons
install -m 644 %{_sourcedir}/hypermark.desktop %{buildroot}/usr/share/applications/hypermark.desktop
install -m 644 %{_sourcedir}/hypermark.svg %{buildroot}/usr/share/icons/hicolor/scalable/apps/hypermark.svg
install -m 644 %{_sourcedir}/hypermark.png %{buildroot}/usr/share/icons/hicolor/256x256/apps/hypermark.png

%files
/usr/bin/hypermark
/usr/share/hypermark
/usr/share/applications/hypermark.desktop
/usr/share/icons/hicolor/scalable/apps/hypermark.svg
/usr/share/icons/hicolor/256x256/apps/hypermark.png

%changelog
* Mon Oct 20 2026 HyperMark Team <info@hypermark.app> - 1.0.0-1.fc44
- Initial release for Fedora 44 Linux.
