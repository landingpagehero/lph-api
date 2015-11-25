package models

case class Branch(private val name: String) {
  override def toString = name
}

object Branch {
  implicit def fromString(name: String) = Branch(name)
}
