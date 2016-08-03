jpa-entitycloner 1.0
============
JPA is a library for cloning a persistent entity including the entire tree relationships thereof.
The jpa-entitycloner not only copies the attributes of an object to another. This library looks at the whole context of
relationships based on jpa annotations as @OneToOne, @OneToMany, @ManyToMany, @ManyToOne, @Embeddable, @Id, @Entity,
and so on applying copies of the fields or using the original value according to the context.

Usage
-------
```java
MyEntityClass entity = entityDAO.find(id);
EntityCloner<MyEntityClass> cloner = new EntityCloner<MyEntityClass>(entity);
MyEntityClass clone = cloner.generateClone();
```

Customize entity clone
-------
The library provides two annotations to customize clone:
* @ForceClone: Fields with this annotation must be cloned even if the clone is not required.
* @IgnoreClone: Fields with this note will not be cloned even if the clone is required.

```java
@Entity
public class MyEntityClass {

	@Id
	private Long id;

	//Will be cloned
	@Column
	private String field1;

	//Must be cloned necessarily independent of context
	@ForceClone
	private String field2;

	//Will be ignored necessarily independent of context and the original value necessarily always will be setted.
	@IgnoreClone
	private String field3;

	//Will be ignored necessarily independent of context and null will be setted in clone object
	@IgnoreClone(setNull = true)
	private String field4;

	//Clone entire relationship tree evaluating the context based on the annotations.
	//The library will seek @OneToMany, @ManyToOne, @ManyToMany, @OneToOne and @Embeddable annotations.
	@OneToMany(mappedBy = "myEntityClass")
	private Set<OtherEntity> list;

	//....
}
```

Download
-------
[Via the releases tab](https://github.com/vlindberg/jpa-entitycloner/releases)

Contact
-------
* Author: Victor Lindberg
* Email: victorlindberg713@gmail.com
