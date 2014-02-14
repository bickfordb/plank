all:



run: dist/plank.jar
	java -cp dist/plank.jar plank.Main $(action)


dist/plank.jar: src/plank/*.java src/plank/*.clj
	rm -rf build dist
	mkdir -p dist build
	unzip lib/clojure-1.5.1.jar -d build
	javac -cp lib/clojure-1.5.1.jar -d build $$(find src -iname '*.java*')
	cp src/plank/*.clj build/plank
	jar cmf MANIFEST.MF $@ -C build .

all: dist/plank.jar

clean:
	rm -rf dist
	rm -rf dist/plank.jar

.PHONY: clean
.PHONY: all
