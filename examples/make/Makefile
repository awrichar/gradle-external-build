PWD := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

OUTPUT := hello
OBJECTS := hello.o

all: $(OUTPUT)

install:
	mkdir -p $(OUTPUT_DIR)
	cp $(OUTPUT) $(OUTPUT_DIR)

clean:
	rm -f $(OBJECTS) $(OUTPUT)

$(OUTPUT): $(OBJECTS)
	gcc -o $@ $^

%.o: $(PWD)/src/%.c
	gcc -c -o $@ $^
