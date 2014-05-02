# to compile:  make FLASCC=/cygdrive/c/flascc/sdk FLEX=/cygdrive/c/flex

all: zip.swc

.SECONDEXPANSION:
%.swc: $$(wildcard %/*)
	echo "$(FLASCC)/usr/bin/g++" $(BASE_CFLAGS) -O4 $*/*.cpp -emit-swc=weave.$* -o $@

clean:
	rm -f *.swf *.swc *.bc *.exe

# http://www.cprogramming.com/tutorial/makefiles_continued.html
# http://stackoverflow.com/questions/6637088/makefile-pattern-rule-referencing-stem-in-dependencies
# http://www.gnu.org/software/make/manual/html_node/Secondary-Expansion.html
