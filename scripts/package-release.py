#!/usr/bin/env python3
"""Build and smoke-test a native installer on its target OS. Uses JDK 21+ and Maven."""
import argparse, hashlib, json, os, pathlib, platform, shutil, subprocess, tempfile, xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parents[1]

def run(args, **kw):
    print('+', ' '.join(str(a) for a in args), flush=True)
    command=[str(a) for a in args]
    if os.name=='nt' and command[0].lower().endswith('.cmd'): command=[os.environ.get('COMSPEC','cmd.exe'),'/d','/c']+command
    subprocess.run(command, cwd=ROOT, check=True, **kw)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--version',required=True);parser.add_argument('--image-only',action='store_true');args=parser.parse_args()
    import re
    if not re.fullmatch(r'[1-9][0-9]*\.[0-9]+\.[0-9]+',args.version):raise SystemExit('Expected a stable x.y.z version')
    version=ET.parse(ROOT/'pom.xml').findtext('{http://maven.apache.org/POM/4.0.0}version')
    if version!=args.version:raise SystemExit('POM and release versions differ')
    system={'Windows':'windows','Darwin':'macos'}.get(platform.system())
    if not system:raise SystemExit('Build installers on Windows or macOS')
    arch={'AMD64':'x64','x86_64':'x64','arm64':'arm64','aarch64':'arm64'}.get(platform.machine())
    if not arch:raise SystemExit('Unsupported build architecture')
    home=pathlib.Path(os.environ['JAVA_HOME']);binary=home/'bin';suffix='.exe' if system=='windows' else ''
    mvn=shutil.which('mvn.cmd' if system=='windows' else 'mvn')
    if not mvn:raise SystemExit('Maven is required')
    (ROOT/'build').mkdir(exist_ok=True);work=pathlib.Path(tempfile.mkdtemp(prefix='native-release-',dir=ROOT/'build'));jars=work/'input';libs=jars/'lib';libs.mkdir(parents=True)
    run([mvn,'-B','-DskipTests','package','org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies','-DincludeScope=runtime',f'-DoutputDirectory={libs}'])
    main_jar=f'fileflow-{version}.jar';shutil.copy2(ROOT/'target'/main_jar,jars/main_jar)
    shutil.copy2(ROOT/'THIRD_PARTY_VIDEO.md',jars/'THIRD_PARTY_VIDEO.md');shutil.copy2(ROOT/'tools/ffmpeg/LICENSE.txt',jars/'FFmpeg-LICENSE.txt')
    icons=work/'icons';run([binary/('java'+suffix),ROOT/'scripts/PackagingIcons.java',ROOT/'src/main/resources/icons/fileflow.png',icons])
    image_dir=work/'image';image_dir.mkdir();jpackage=binary/('jpackage'+suffix)
    command=[jpackage,'--type','app-image','--name','FileFlow','--app-version',version,'--vendor','FileFlow','--description','Organise your local files with FileFlow',
             '--input',jars,'--main-jar',main_jar,'--main-class','com.austin.fileflow.Launcher','--dest',image_dir,'--icon',icons/('fileflow.ico' if system=='windows' else 'fileflow.icns'),
             '--java-options','-Dfile.encoding=UTF-8','--java-options','--enable-native-access=ALL-UNNAMED']
    if system=='macos':command+=['--mac-package-identifier','com.austin.fileflow','--java-options','-Dapple.awt.application.name=FileFlow']
    run(command)
    app=image_dir/('FileFlow' if system=='windows' else 'FileFlow.app')
    if system=='macos':run(['/usr/bin/codesign','--force','--deep','--sign','-',app])
    executable=app/('FileFlow.exe' if system=='windows' else 'Contents/MacOS/FileFlow');report=work/'smoke.json'
    run([executable,'--self-test',report],timeout=90)
    smoke=json.loads(report.read_text(encoding='utf-8'));assert smoke['version']==version and smoke['javafx'] and smoke['sqlite'],smoke
    expected={'windows':'UNDECORATED','macos':('UNIFIED','DECORATED')}[system]
    assert smoke['stageStyle'] in ((expected,) if isinstance(expected,str) else expected),smoke
    print('Native application smoke test passed:',smoke,flush=True)
    if args.image_only:return
    dest=ROOT/'dist';dest.mkdir(exist_ok=True)
    package=[jpackage,'--type','msi' if system=='windows' else 'dmg','--app-image',app,'--name','FileFlow','--app-version',version,'--dest',dest,'--vendor','FileFlow']
    if system=='windows':package+=['--win-per-user-install','--win-dir-chooser','--win-menu','--win-shortcut','--win-upgrade-uuid','6419ec91-5288-42da-b310-fcdf688b6b42']
    else:package+=['--mac-package-identifier','com.austin.fileflow','--mac-package-name','FileFlow']
    run(package)
    ext='msi' if system=='windows' else 'dmg';original=dest/f'FileFlow-{version}.{ext}';target=dest/f'FileFlow-{version}-{system}-{arch}.{ext}'
    if target.exists():raise SystemExit('Refusing to overwrite an existing installer')
    original.rename(target)
    checksum=hashlib.sha256(target.read_bytes()).hexdigest();target.with_name(target.name+'.sha256').write_text(checksum+'  '+target.name+'\n',encoding='utf-8')
    target.with_name(target.name+'.smoke.json').write_text(json.dumps(smoke,indent=2),encoding='utf-8')
    print('Installer:',target,'SHA256:',checksum,flush=True)
if __name__=='__main__':main()

