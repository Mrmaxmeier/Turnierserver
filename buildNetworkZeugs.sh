#!/bin/bash

if [ -d build ]
then
    rm -rf build
fi
mkdir -p build

projects="NetworkingLib CompilerLib Worker Game-Logic Backend"
for project in $projects
do
    cd $project
    #mvn clean
    mvn package install dependency:copy-dependencies
    cp target/*.jar ../build/
    cp target/dependency/* ../build/
    cd ..
done

echo -e "#!/bin/sh\ncd \`dirname \$0\`\njava -cp '*' org.pixelgaffer.turnierserver.backend.BackendMain \${@}" > build/backend.sh
echo -e "#!/bin/sh\ncd \`dirname \$0\`\njava -cp '*' org.pixelgaffer.turnierserver.worker.WorkerMain \${@}" > build/worker.sh
chmod +x build/*.sh
