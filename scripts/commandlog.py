import os

def log_command(command, description='', outfile='logs/commands.txt'):
    os.makedirs(os.path.dirname(outfile), exist_ok=True)
    
    outstr = "%s: %s" % (description, command) if description != '' else command
    
    with open(outfile, "a") as f:
        f.write("%s\n" % outstr)