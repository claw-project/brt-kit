module br_test

contains
  subroutine sub1()
    real :: a, b, c
    integer :: d, e
    real, parameter :: y = 2 ** PRECISION(10.0)

    c = sin(a)
    c = cos(a)
    b = c ** a

    e = ishift(d)

  end subroutine sub1

end module br_test
