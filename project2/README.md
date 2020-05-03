# Μεταγλωττιστές - άνοιξη 2020

### Μάρκος Βαρβαγιάννης
### sdi1400017@di.uoa.gr

## 2η Εργασία - MiniJava Static Checking (Semantic Analysis)

### Σχεδιαστικές Επιλογές
- Έχουν χρησιμοποιηθεί δύο visitors, όπως προτάθηκε στο μάθημα, ένας για να γεμίσει τον symbol table (*FillTableVisitor.java*) και ένας που διατρέχει τον κώδικα και κάνει το type checking (*TypeCheckingVisitor.java*).
- Το αρχείο *SymbolTable.java* περιέχει τη δομή του Symbol Table που αποτελείται από 3 κλάσεις, μία για τον symbol table, μία για τα class contents και μία για τα method contents.
- Η δομή που χρησιμοποιείται για τον symbol table είναι *LinkedHashMap*.
- O Visitor που χρησιμοποιείται είναι ο *GJDepthFirst*
- Η main class όπως και η main method εισάγονται στον symbol table για λόγους συνέπειας του type checking, αλλά δεν τυπώνονται στα offsets.
- Για τα expression terms του MessageSend, χρησιμοποιείται μία στοίβα *argument_stack* στην κλάση του TypeCheckingVisitor. Πιο συγκεκριμένα, κάθε φορά που μπαίνουμε σε ένα νέο expression list προστίθεται στη στοίβα το σύμβολο '(' ώστε να γίνεται σαφής διαχωρισμός των ορισμάτων στα nested calls.

#### Σημείωση
Καθώς είναι η δεύτερη χρονιά που παρακολουθώ το μάθημα, ένα μέρος του κώδικα προέρχεται από την περσινή μου εργασία (που την είχα γράψει εγώ ο ίδιος).