# Contributing to JobFlow

We love your input! We want to make contributing to JobFlow as easy and transparent as possible, whether it's:

- Reporting a bug
- Discussing the current state of the code
- Submitting a fix
- Proposing new features
- Becoming a maintainer

## We Develop with Github
We use GitHub to host code, to track issues and feature requests, as well as accept pull requests.

## We Use [Github Flow](https://guides.github.com/introduction/flow/index.html)
Pull requests are the best way to propose changes to the codebase. We actively welcome your pull requests:

1. Fork the repo and create your branch from `main`.
2. If you've added code that should be tested, add tests.
3. If you've changed APIs, update the documentation.
4. Ensure the test suite passes.
5. Make sure your code lints.
6. Issue that pull request!

## Any contributions you make will be under the MIT Software License
In short, when you submit code changes, your submissions are understood to be under the same [MIT License](http://choosealicense.com/licenses/mit/) that covers the project. Feel free to contact the maintainers if that's a concern.

## Report bugs using Github's [issue tracker](https://github.com/yourusername/jobflow/issues)
We use GitHub issues to track public bugs. Report a bug by [opening a new issue](https://github.com/yourusername/jobflow/issues/new); it's that easy!

## Write bug reports with detail, background, and sample code

**Great Bug Reports** tend to have:

- A quick summary and/or background
- Steps to reproduce
  - Be specific!
  - Give sample code if you can.
- What you expected would happen
- What actually happens
- Notes (possibly including why you think this might be happening, or stuff you tried that didn't work)

## Development Process

1. Clone the repository
```bash
git clone https://github.com/yourusername/jobflow.git
```

2. Create a branch
```bash
git checkout -b feature/your-feature-name
```

3. Set up development environment
```bash
# Install dependencies
mvn clean install

# Run tests
mvn test

# Start the application
mvn spring-boot:run
```

4. Make your changes and commit them
```bash
git add .
git commit -m "Description of changes"
```

5. Push to your fork and submit a pull request
```bash
git push origin feature/your-feature-name
```

## Code Style Guidelines

- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Write clear comments and documentation
- Keep methods focused and concise
- Write unit tests for new code

## Testing Guidelines

- Write unit tests for all new code
- Maintain test coverage above 80%
- Use meaningful test names that describe the behavior being tested
- Follow the AAA (Arrange, Act, Assert) pattern
- Mock external dependencies appropriately

## Documentation Guidelines

- Keep README.md up to date
- Document all public APIs
- Include examples in documentation
- Update documentation when making changes
- Use clear and concise language

## Pull Request Process

1. Update the README.md with details of changes if needed
2. Update the version numbers in any examples files and the README.md to the new version
3. The PR will be merged once you have the sign-off of at least one maintainer

## Community

- Be welcoming to newcomers and encourage diverse new contributors
- Be respectful of different viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

## License
By contributing, you agree that your contributions will be licensed under its MIT License.

## References
This document was adapted from the open-source contribution guidelines for [Facebook's Draft](https://github.com/facebook/draft-js/blob/a9316a723f9e918afde44dea68b5f9f39b7d9b00/CONTRIBUTING.md).
