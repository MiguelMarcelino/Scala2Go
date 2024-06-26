package base

import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.eclipse.jface.text.Document
import org.eclipse.text.edits.{TextEdit, UndoEdit}

import scala.collection.mutable.ListBuffer

/** Encapsulates the objects needed to perform a rewrite on the AST
  * Contains generic methods to add and replace nodes in the AST. More specific transformations must be implemented
  * as extensions to this class.
  * @param rewriter the ASTRewrite object
  * @param listRewrite the ListRewrite object
  */
abstract class ASTTransformer(document: Document) extends ASTVisitor {

  private final val astEdits: ListBuffer[UndoEdit] = ListBuffer.empty[UndoEdit]

  // TODO: This should be a parameter
  final val parser = ASTParser.newParser(AST.JLS9)

  def transform(): Unit = {
    parser.setSource(document.get.toCharArray)
    val compilationUnit = parser.createAST(null).asInstanceOf[CompilationUnit]
    compilationUnit.accept(this)
  }

  def getTransformations(): Seq[UndoEdit] = astEdits.toSeq

  /** Adds a new element to the provided ASTNode
    * @param element the element to add
    * @return the `UndoEdit` object encapsulating the reverse changes of the edit
    */
  def appendElement(astNode: ASTNode): TextEdit = {
    // Create the rewriter objects
    val rewriteObjects = getRewriteObjects()

    // Insert into AST
    rewriteObjects.listRewrite.insertLast(astNode, null)

    // Apply edit
    val edit = rewriteObjects.rewriter.rewriteAST(document, null)
    applyTransformation(edit)
  }

  /** Replaces an element from the provided ASTNode
    * @param element the element to replace
    * @return the `UndoEdit` object encapsulating the reverse changes of the edit
    */
  def replaceElement(astNode: ASTNode, replacement: ASTNode): UndoEdit = {
    // Create the rewriter objects
    val rewriteObjects = getRewriteObjects()

    // Replace element in AST
    rewriteObjects.listRewrite.replace(astNode, replacement, null)

    // Apply edit
    val edit = rewriteObjects.rewriter.rewriteAST(document, null)
    applyTransformation(edit)
  }

  /** Applies a transformation to the AST
    *
    * @param ast  the AST to apply the transformations to
    * @param edit the edit to apply
    * @return the `UndoEdit` object encapsulating the reverse changes of the edit
    */
  protected def applyTransformation(edit: TextEdit): UndoEdit = {
    val undoEdit = edit.apply(document)

    // Keep track of all reverse changes of all the edits, to allow reverting them if necessary
    astEdits.append(undoEdit)

    undoEdit
  }

  /** Creates a new ASTRewrite object for the given AST
    * @param ast the AST to create the ASTRewrite object for
    * @return the ASTRewrite object
    */
  private def getRewriteObjects(): RewriteObjects = {
    parser.setSource(document.get.toCharArray)
    val compilationUnit = parser.createAST(null).asInstanceOf[CompilationUnit]
    val ast = compilationUnit.getAST
    val rewriter = ASTRewrite.create(ast)
    val lrw =
      rewriter.getListRewrite(compilationUnit, CompilationUnit.TYPES_PROPERTY)

    RewriteObjects(
      rewriter = rewriter,
      listRewrite = lrw
    )
  }

}
